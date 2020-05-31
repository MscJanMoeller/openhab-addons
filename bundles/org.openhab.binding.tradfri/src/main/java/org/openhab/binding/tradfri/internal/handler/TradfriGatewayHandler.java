/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.tradfri.internal.handler;

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.eclipse.californium.scandium.AlertHandler;
import org.eclipse.californium.scandium.ConnectionListener;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.Connection;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.tradfri.internal.CoapCallback;
import org.openhab.binding.tradfri.internal.TradfriBindingConstants;
import org.openhab.binding.tradfri.internal.TradfriCoapClient;
import org.openhab.binding.tradfri.internal.config.TradfriGatewayConfig;
import org.openhab.binding.tradfri.internal.discovery.TradfriDiscoveryService;
import org.openhab.binding.tradfri.internal.handler.TradfriResourceListEventListener.ResourceListEvent;
import org.openhab.binding.tradfri.internal.model.TradfriDevice;
import org.openhab.binding.tradfri.internal.model.TradfriGatewayData;
import org.openhab.binding.tradfri.internal.model.TradfriVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link TradfriGatewayHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class TradfriGatewayHandler extends BaseBridgeHandler implements ConnectionListener, AlertHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private static final TradfriVersion MIN_SUPPORTED_VERSION = new TradfriVersion("1.2.42");

    private static final int ACCEPTED_PING_LOSSES = 1;

    private static final Gson gson = new Gson();

    private @NonNullByDefault({}) String gatewayURI;

    private @NonNullByDefault({}) CoapClient gatewayClient;
    private @NonNullByDefault({}) TradfriCoapClient gatewayInfoClient;

    private @NonNullByDefault({}) DTLSConnector dtlsConnector;
    private @Nullable CoapEndpoint endPoint;

    private @NonNullByDefault({}) TradfriResourceListObserver deviceListObserver;
    private @NonNullByDefault({}) TradfriResourceListObserver groupListObserver;
    private @NonNullByDefault({}) TradfriResourceListObserver sceneListObserver;

    private final TradfriDiscoveryService discoveryService;

    private @Nullable ScheduledFuture<?> supvJob;

    private int pingLosses = 0;

    private @Nullable TradfriGatewayData gatewayData;

    public TradfriGatewayHandler(Bridge bridge, TradfriDiscoveryService ds) {
        super(bridge);
        this.discoveryService = ds;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // there are no channels on the gateway yet
    }

    @Override
    public void initialize() {
        TradfriGatewayConfig configuration = getConfigAs(TradfriGatewayConfig.class);

        if (isNullOrEmpty(configuration.host)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Host must be specified in the configuration!");
            return;
        }

        // Create coap client for getting details about the gateway
        try {
            // Use class URI to validate host
            URI uri = new URI("coaps://" + configuration.host + ":" + configuration.port + "/");
            this.gatewayURI = uri.toString();
            this.gatewayClient = new CoapClient(uri);
            this.gatewayInfoClient = new TradfriCoapClient(uri.toString() + ENDPOINT_GATEWAY_DETAILS);
        } catch (URISyntaxException e) {
            logger.error("Illegal gateway URI '{}': {}", this.gatewayURI, e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            return;
        }

        if (isNullOrEmpty(configuration.code)) {
            if (isNullOrEmpty(configuration.identity) || isNullOrEmpty(configuration.preSharedKey)) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Either security code or identity and pre-shared key must be provided in the configuration!");
                return;
            } else {
                establishConnection();
            }
        } else {
            String currentFirmware = thing.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION);
            if (!isNullOrEmpty(currentFirmware)
                    && MIN_SUPPORTED_VERSION.compareTo(new TradfriVersion(currentFirmware)) > 0) {
                // older firmware not supported
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        String.format(
                                "Gateway firmware version '%s' is too old! Minimum supported firmware version is '%s'.",
                                currentFirmware, MIN_SUPPORTED_VERSION.toString()));
                return;
            }

            // Running async operation to retrieve new <'identity','key'> pair
            scheduler.execute(() -> {
                boolean success = obtainIdentityAndPreSharedKey();
                if (success) {
                    establishConnection();
                }
            });
        }
    }

    /**
     * Authenticates against the gateway with the security code in order to receive a pre-shared key for a newly
     * generated identity.
     * As this requires a remote request, this method might be long-running.
     *
     * @return true, if credentials were successfully obtained, false otherwise
     */
    protected boolean obtainIdentityAndPreSharedKey() {
        TradfriGatewayConfig configuration = getConfigAs(TradfriGatewayConfig.class);

        String identity = UUID.randomUUID().toString().replace("-", "");
        String preSharedKey = null;

        CoapResponse gatewayResponse;
        String authUrl = null;
        String responseText = null;
        try {
            DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder();
            builder.setPskStore(new StaticPskStore("Client_identity", configuration.code.getBytes()));

            DTLSConnector dtlsConnector = new DTLSConnector(builder.build());
            CoapEndpoint.Builder authEndpointBuilder = new CoapEndpoint.Builder();
            authEndpointBuilder.setConnector(dtlsConnector);
            CoapEndpoint authEndpoint = authEndpointBuilder.build();
            authUrl = "coaps://" + configuration.host + ":" + configuration.port + "/15011/9063";

            CoapClient deviceClient = new CoapClient(new URI(authUrl));
            deviceClient.setTimeout(TimeUnit.SECONDS.toMillis(10));
            deviceClient.setEndpoint(authEndpoint);

            JsonObject json = new JsonObject();
            json.addProperty(CLIENT_IDENTITY_PROPOSED, identity);

            gatewayResponse = deviceClient.post(json.toString(), 0);

            authEndpoint.destroy();
            deviceClient.shutdown();

            if (gatewayResponse == null) {
                // seems we ran in a timeout, which potentially also happens
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "No response from gateway. Might be due to an invalid security code.");
                return false;
            }

            if (gatewayResponse.isSuccess()) {
                responseText = gatewayResponse.getResponseText();
                json = new JsonParser().parse(responseText).getAsJsonObject();
                preSharedKey = json.get(NEW_PSK_BY_GW).getAsString();

                if (isNullOrEmpty(preSharedKey)) {
                    logger.error("Received pre-shared key is empty for thing {} on gateway at {}", getThing().getUID(),
                            configuration.host);
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Pre-shared key was not obtain successfully");
                    return false;
                } else {
                    logger.info("Received pre-shared key for gateway '{}'", configuration.host);
                    logger.debug("Using identity '{}' with pre-shared key '{}'.", identity, preSharedKey);

                    Configuration editedConfig = editConfiguration();
                    editedConfig.put(TradfriBindingConstants.GATEWAY_CONFIG_CODE, null);
                    editedConfig.put(TradfriBindingConstants.GATEWAY_CONFIG_IDENTITY, identity);
                    editedConfig.put(TradfriBindingConstants.GATEWAY_CONFIG_PRE_SHARED_KEY, preSharedKey);
                    updateConfiguration(editedConfig);

                    return true;
                }
            } else {
                logger.warn(
                        "Failed obtaining pre-shared key for identity '{}' (response code '{}', response text '{}')",
                        identity, gatewayResponse.getCode(),
                        isNullOrEmpty(gatewayResponse.getResponseText()) ? "<empty>"
                                : gatewayResponse.getResponseText());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, String
                        .format("Failed obtaining pre-shared key with status code '%s'", gatewayResponse.getCode()));
            }
        } catch (URISyntaxException e) {
            logger.error("Illegal gateway URI '{}'", authUrl, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
        } catch (JsonParseException e) {
            logger.warn("Invalid response received from gateway '{}'", responseText, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    String.format("Invalid response received from gateway '%s'", responseText));
        } catch (ConnectorException | IOException e) {
            logger.debug("Error connecting to gateway ", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    String.format("Error connecting to gateway."));
        }
        return false;
    }

    private void establishConnection() {
        TradfriGatewayConfig configuration = getConfigAs(TradfriGatewayConfig.class);

        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder();
        builder.setPskStore(new StaticPskStore(configuration.identity, configuration.preSharedKey.getBytes()));
        builder.setMaxConnections(100);
        // builder.setStaleConnectionThreshold(60);
        builder.setConnectionListener(this);
        this.dtlsConnector = new DTLSConnector(builder.build());
        this.dtlsConnector.setAlertHandler(this);
        this.endPoint = new CoapEndpoint.Builder().setConnector(this.dtlsConnector).build();

        if (this.gatewayClient != null) {
            this.gatewayClient.setEndpoint(getEndpoint());
        }

        if (this.gatewayInfoClient != null) {
            this.gatewayInfoClient.setEndpoint(getEndpoint());
        }

        // Schedule a CoAP ping every minute to check the connection
        supvJob = scheduler.scheduleWithFixedDelay(this::checkConnection, 0, 1, TimeUnit.MINUTES);

        updateStatus(ThingStatus.UNKNOWN);
    }

    private void checkConnection() {
        if (this.gatewayClient != null && getEndpoint() != null) {
            if (!this.gatewayClient.ping(TradfriCoapClient.TIMEOUT)) {
                tryResumeConnection();
                if (this.pingLosses++ > ACCEPTED_PING_LOSSES) {
                    String detailedError = MessageFormat.format("{0} CoAP pings lost. Gateway seems to be down.",
                            this.pingLosses);
                    onConnectionError(detailedError);
                }
            } else {
                this.pingLosses = 0;
            }
        }
    }

    private void tryResumeConnection() {
        // To fix connection issues after a gateway reboot, a session resume is forced for the next CoAP message
        logger.debug("Gateway communication error. Forcing session resume on next command.");
        TradfriGatewayConfig configuration = getConfigAs(TradfriGatewayConfig.class);
        InetSocketAddress peerAddress = new InetSocketAddress(configuration.host, configuration.port);
        this.dtlsConnector.forceResumeSessionFor(peerAddress);
    }

    @Override
    public void onConnectionEstablished(@Nullable Connection connection) {
        logger.debug("DTLS connection established successfully");

        // Requesting info about the gateway and add firmware version
        requestGatewayInfo();
    }

    @Override
    public void onConnectionRemoved(@Nullable Connection connection) {
        logger.debug("DTLS connection removed");
        // Restart endpoint
        if (this.endPoint != null) {
            try {
                this.endPoint.stop();
                this.endPoint.start();
                logger.debug("Started CoAP endpoint {}", this.endPoint.getAddress());
            } catch (IOException e) {
                logger.error("Could not start CoAP endpoint", e);
            }
        }
    }

    @Override
    public void onAlert(@Nullable InetSocketAddress peer, @Nullable AlertMessage alert) {
        if (peer != null && alert != null) {
            String detailedError = MessageFormat.format("Failed connecting to {0}: {1}", peer.toString(),
                    alert.getDescription());
            onConnectionError(detailedError);
        }
    }

    public void onConnectionError(String detailedError) {
        if (!isNullOrEmpty(detailedError)) {
            logger.warn(detailedError);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, detailedError);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
    }

    @Override
    public void dispose() {
        stopScan();

        if (supvJob != null) {
            supvJob.cancel(true);
            supvJob = null;
        }
        if (endPoint != null) {
            endPoint.destroy();
            endPoint = null;
        }
        if (gatewayClient != null) {
            gatewayClient.shutdown();
            gatewayClient = null;
        }
        if (gatewayInfoClient != null) {
            gatewayInfoClient.shutdown();
            gatewayInfoClient = null;
        }
        super.dispose();
    }

    /**
     * Does requests to the gateway to list all available devices, groups and scenes.
     */
    public synchronized void startScan() {
        stopScan();

        // Create observer for devices, groups and scenes and observe lists

        this.deviceListObserver = new TradfriResourceListObserver(getGatewayURI() + "/" + ENDPOINT_DEVICES,
                getEndpoint(), scheduler);
        this.deviceListObserver.registerListener((event, id) -> {
            if (event == ResourceListEvent.RESOURCE_ADDED) {
                TradfriResourceObserver<TradfriDevice> observer = new TradfriDeviceObserver(
                        getGatewayURI() + "/" + ENDPOINT_DEVICES + "/" + id, getEndpoint(), scheduler);
                observer.registerListener((resourceData) -> {
                    this.discoveryService.onDeviceUpdate(getThing(), resourceData.getInstanceID(),
                            gson.toJsonTree(resourceData).getAsJsonObject());
                });
                observer.observe();
            }
        });
        this.deviceListObserver.observe();

        this.groupListObserver = new TradfriResourceListObserver(getGatewayURI() + "/" + ENDPOINT_GROUPS, getEndpoint(),
                scheduler);
        this.groupListObserver.observe();

        this.sceneListObserver = new TradfriResourceListObserver(getGatewayURI() + "/" + ENDPOINT_SCENES, getEndpoint(),
                scheduler);
        this.sceneListObserver.observe();
    }

    public synchronized void stopScan() {

        if (this.deviceListObserver != null) {
            this.deviceListObserver.dispose();
            this.deviceListObserver = null;
        }
        if (this.groupListObserver != null) {
            this.groupListObserver.dispose();
            this.groupListObserver = null;
        }
        if (this.sceneListObserver != null) {
            this.sceneListObserver.dispose();
            this.sceneListObserver = null;
        }

    }

    /**
     * Returns the root URI of the gateway.
     *
     * @return root URI of the gateway with coaps scheme
     */
    public String getGatewayURI() {
        return gatewayURI;
    }

    /**
     * Returns the coap endpoint that can be used within coap clients.
     *
     * @return the coap endpoint
     */
    public @Nullable CoapEndpoint getEndpoint() {
        return endPoint;
    }

    private synchronized void requestGatewayInfo() {
        gatewayInfoClient.asyncGet(new CoapCallback() {
            @Override
            public void onUpdate(JsonElement data) {
                logger.debug("requestGatewayInfo response: {}", data);
                try {
                    gatewayData = gson.fromJson(data, TradfriGatewayData.class);
                    getThing().setProperty(Thing.PROPERTY_FIRMWARE_VERSION, gatewayData.getVersion());
                } catch (JsonSyntaxException ex) {
                    logger.error("Unexpected requestGatewayInfo response: {}", data);
                }
            }

            @Override
            public void onError(ThingStatus status, ThingStatusDetail statusDetail) {
                logger.error("CoAP error: requestGatewayInfo failed");
            }
        });
    }

    private boolean isNullOrEmpty(@Nullable String string) {
        return string == null || string.isEmpty();
    }

    @Override
    public void thingUpdated(Thing thing) {
        super.thingUpdated(thing);

        logger.info("Bridge configuration updated. Updating paired things (if any).");
        for (Thing t : getThing().getThings()) {
            final ThingHandler thingHandler = t.getHandler();
            if (thingHandler != null) {
                thingHandler.thingUpdated(t);
            }
        }
    }

}
