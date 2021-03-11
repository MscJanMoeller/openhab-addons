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
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
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
import org.openhab.binding.tradfri.internal.coap.TradfriCoapClient;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapResourceCache;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapGateway;
import org.openhab.binding.tradfri.internal.config.TradfriGatewayConfig;
import org.openhab.binding.tradfri.internal.discovery.TradfriDiscoveryService;
import org.openhab.binding.tradfri.internal.model.TradfriDevice;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.openhab.binding.tradfri.internal.model.TradfriEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriGroup;
import org.openhab.binding.tradfri.internal.model.TradfriResourceCache;
import org.openhab.binding.tradfri.internal.model.TradfriVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * The {@link TradfriGatewayHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Jan Möller - Refactoring to increase stability
 * @author Jan Möller - Added support of native TRADFRI groups and scenes
 */
@NonNullByDefault
public class TradfriGatewayHandler extends BaseBridgeHandler implements ConnectionListener, AlertHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private static final TradfriVersion MIN_SUPPORTED_VERSION = new TradfriVersion("1.2.42");

    private static final int ACCEPTED_COAP_ERRORS = 1;

    private static final Gson GSON = new Gson();

    private @Nullable URI gatewayURI;

    private @Nullable TradfriCoapClient gatewayClient;

    private @Nullable Endpoint endpoint;

    private final TradfriDiscoveryService discoveryService;

    private final TradfriCoapResourceCache resourceCache;

    private final AtomicInteger activeScans = new AtomicInteger(0);

    private @Nullable ScheduledFuture<?> supvJob;

    private int pingLosses = 0;
    private int coapErrors = 0;

    public TradfriGatewayHandler(Bridge bridge, TradfriDiscoveryService ds) {
        super(bridge);

        this.discoveryService = ds;
        this.resourceCache = new TradfriCoapResourceCache();
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

        // Validate host and port parameter of configuration
        try {
            // Use class URI to validate host
            this.gatewayURI = new URI("coaps://" + configuration.host + ":" + configuration.port + "/");
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
                initializeConnection();
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
                    initializeConnection();
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
    private boolean obtainIdentityAndPreSharedKey() {
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
                    editedConfig.put(TradfriGatewayConfig.CONFIG_CODE, null);
                    editedConfig.put(TradfriGatewayConfig.CONFIG_IDENTITY, identity);
                    editedConfig.put(TradfriGatewayConfig.CONFIG_PRE_SHARED_KEY, preSharedKey);
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

    private void initializeConnection() {
        TradfriGatewayConfig configuration = getConfigAs(TradfriGatewayConfig.class);

        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder();
        builder.setPskStore(new StaticPskStore(configuration.identity, configuration.preSharedKey.getBytes()));
        builder.setMaxConnections(100);
        // builder.setStaleConnectionThreshold(60);
        builder.setConnectionListener(this);
        DTLSConnector dtlsConnector = new DTLSConnector(builder.build());
        dtlsConnector.setAlertHandler(this);

        Endpoint endpoint = new CoapEndpoint.Builder().setConnector(dtlsConnector).build();
        this.endpoint = endpoint;

        final URI uri = this.gatewayURI;
        if (uri != null) {
            final CoapClient coapClient = new CoapClient(uri).setEndpoint(endpoint);
            this.gatewayClient = new TradfriCoapClient(uri, coapClient, scheduler);
        }

        // Schedule a CoAP ping every minute to check the connection
        supvJob = scheduler.scheduleWithFixedDelay(this::checkConnection, 0, 1, TimeUnit.MINUTES);
    }

    private void checkConnection() {
        if (this.gatewayClient != null) {
            if (this.gatewayClient.ping()) {
                this.pingLosses = 0;
                updateOnlineStatus();
            } else {
                String detailedError = MessageFormat.format("{0} CoAP pings lost. Gateway seems to be down.",
                        this.pingLosses);
                onConnectionError(detailedError);
            }
        }
    }

    @Override
    public void onConnectionEstablished(@Nullable Connection connection) {
        logger.debug("DTLS connection established successfully");

        // Requesting info about the gateway and add firmware version
        requestGatewayInfo();

        // Connect TradfriDiscoveryService with resource cache to get events for devices and groups
        this.resourceCache.subscribeEvents(this);

        final TradfriCoapClient gwClient = this.gatewayClient;
        if (gwClient != null) {
            this.resourceCache.initialize(gwClient, scheduler);
        }
    }

    @Override
    public void onConnectionRemoved(@Nullable Connection connection) {
        logger.debug("DTLS connection removed");
        resumeConnection();
    }

    @Override
    public void onAlert(@Nullable InetSocketAddress peer, @Nullable AlertMessage alert) {
        if (peer != null && alert != null) {
            String detailedError = MessageFormat.format("Failed connecting to {0}: {1}", peer.toString(),
                    alert.getDescription());
            onConnectionError(detailedError, false);
        }
    }

    private void onConnectionError(String detailedError) {
        onConnectionError(detailedError, true);
    }

    private void onConnectionError(String detailedError, boolean resumeConnection) {
        if (this.coapErrors > ACCEPTED_COAP_ERRORS || !resumeConnection) {
            if (!isNullOrEmpty(detailedError)) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, detailedError);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            }
            if (resumeConnection) {
                resumeConnection();
            }
        }
    }

    private void resumeConnection() {
        Endpoint endpoint = getEndpoint();
        // Restart endpoint
        if (endpoint != null) {
            try {
                endpoint.stop();
                endpoint.start();
                this.coapErrors = 0;
                logger.debug("Started CoAP endpoint {}", endpoint.getAddress());

                // TODO Invalidate all resource proxies?
            } catch (IOException e) {
                logger.error("Could not start CoAP endpoint", e);
            }
        }
    }

    @Override
    public void dispose() {
        stopScan();

        this.resourceCache.unsubscribeEvents(this);

        this.resourceCache.clear();

        if (this.supvJob != null) {
            this.supvJob.cancel(true);
            this.supvJob = null;
        }
        if (this.endpoint != null) {
            this.endpoint.destroy();
            this.endpoint = null;
        }
        if (this.gatewayClient != null) {
            this.gatewayClient.dispose();
            this.gatewayClient = null;
        }

        super.dispose();
    }

    public TradfriResourceCache getResourceCache() {
        return this.resourceCache;
    }

    /**
     * Enables background discovery of devices, groups and scenes
     */
    public void startBackgroundDiscovery() {
        this.activeScans.getAndIncrement();
        logger.trace("Start background discovery. Num active scan: {}", this.activeScans.toString());
    }

    /**
     * Disables background discovery of devices, groups and scenes
     */
    public void stopBackgroundDiscovery() {
        this.activeScans.getAndUpdate(i -> i > 0 ? i - 1 : 0);
        logger.trace("Stop background discovery. Num active scan: {}", this.activeScans.toString());
    }

    /**
     * Forces to scan devices, groups and scenes
     */
    public synchronized void startScan() {
        this.activeScans.getAndIncrement();
        logger.trace("Start discovery. Num active scan: {}", this.activeScans.toString());

        this.resourceCache.refresh();
    }

    /**
     * Stops the scan of devices, groups and scenes
     */
    public synchronized void stopScan() {
        this.activeScans.getAndUpdate(i -> i > 0 ? i - 1 : 0);
        logger.trace("Stop discovery. Num active scan: {}", this.activeScans.toString());
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

    /**
     * Returns the root URI of the gateway.
     *
     * @return root URI of the gateway with coaps scheme
     */
    public @Nullable URI getGatewayURI() {
        return this.gatewayURI;
    }

    /**
     * Returns the coap endpoint that can be used within coap clients.
     *
     * @return the coap endpoint
     */
    public @Nullable Endpoint getEndpoint() {
        return this.endpoint;
    }

    @TradfriEventHandler({ EType.RESOURCE_ADDED, EType.RESOURCE_UPDATED })
    public void onDeviceAddedOrUpdated(TradfriEvent event, TradfriDevice proxy) {
        updateOnlineStatus();
        if (mustNotifyDiscoveryService()) {
            // TODO inform discovery service only if relevant data changed (like name of device)
            this.discoveryService.onDeviceUpdated(getThing(), proxy);
        }
    }

    @TradfriEventHandler({ EType.RESOURCE_ADDED, EType.RESOURCE_UPDATED })
    public void onGroupAddedOrUpdated(TradfriEvent event, TradfriGroup proxy) {
        updateOnlineStatus();
        if (mustNotifyDiscoveryService()) {
            // TODO inform discovery service only if relevant data changed (like name of group)
            this.discoveryService.onGroupUpdated(getThing(), proxy);
        }
    }

    @TradfriEventHandler(EType.RESOURCE_REMOVED)
    public void onGroupRemoved(TradfriEvent event, TradfriGroup proxy) {
        if (mustNotifyDiscoveryService()) {
            this.discoveryService.onGroupRemoved(getThing(), proxy);
        }
    }

    private void updateOnlineStatus() {
        Bridge gateway = getThing();
        ThingStatus status = gateway.getStatus();
        if (status != ThingStatus.ONLINE) {
            boolean hasFwVersion = gateway.getProperties().containsKey(Thing.PROPERTY_FIRMWARE_VERSION);
            if (hasFwVersion && this.resourceCache.isInitialized()) {
                updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);
            }
        }
    }

    private void requestGatewayInfo() {
        if (this.gatewayClient != null) {
            this.gatewayClient.get(ENDPOINT_GATEWAY_DETAILS, new CoapHandler() {
                @Override
                public void onLoad(@Nullable CoapResponse response) {
                    if (response == null) {
                        logger.trace("Received empty GatewayInfo CoAP response");
                        return;
                    }

                    logger.trace("GatewayInfo CoAP response\noptions: {}\npayload: {}", response.getOptions(),
                            response.getResponseText());
                    if (response.isSuccess()) {
                        try {
                            TradfriCoapGateway gateway = GSON.fromJson(response.getResponseText(),
                                    TradfriCoapGateway.class);
                            getThing().setProperty(Thing.PROPERTY_FIRMWARE_VERSION, gateway.getVersion());
                            updateOnlineStatus();
                        } catch (JsonParseException ex) {
                            logger.error("Unexpected requestGatewayInfo response: {}", response);
                        }
                    } else {
                        logger.error("GatewayInfo CoAP error: {}", response.getCode());
                    }
                }

                @Override
                public void onError() {
                    logger.error("CoAP error: requestGatewayInfo failed");
                }
            });
        }
    }

    private boolean mustNotifyDiscoveryService() {
        return this.activeScans.get() > 0;
    }

    private boolean isNullOrEmpty(@Nullable String string) {
        return string == null || string.isEmpty();
    }

}
