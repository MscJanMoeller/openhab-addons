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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.eclipse.jdt.annotation.NonNull;
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
import org.openhab.binding.tradfri.internal.coap.CoapCallback;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapClient;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapDeviceProxy;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapGroupProxy;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapResourceProxy;
import org.openhab.binding.tradfri.internal.coap.TradfriResourceListEventHandler.ResourceListEvent;
import org.openhab.binding.tradfri.internal.coap.TradfriResourceListObserver;
import org.openhab.binding.tradfri.internal.config.TradfriGatewayConfig;
import org.openhab.binding.tradfri.internal.discovery.TradfriDiscoveryService;
import org.openhab.binding.tradfri.internal.model.TradfriDevice;
import org.openhab.binding.tradfri.internal.model.TradfriGatewayData;
import org.openhab.binding.tradfri.internal.model.TradfriGroup;
import org.openhab.binding.tradfri.internal.model.TradfriResource;
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
 * @author Jan Möller - Refactoring to increase stability and support of native TRADFRI groups and scenes
 */
@NonNullByDefault
public class TradfriGatewayHandler extends BaseBridgeHandler implements ConnectionListener, AlertHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private static final TradfriVersion MIN_SUPPORTED_VERSION = new TradfriVersion("1.2.42");

    private static final int ACCEPTED_COAP_ERRORS = 1;

    private static final Gson gson = new Gson();

    private @NonNullByDefault({}) String gatewayURI;

    private @NonNullByDefault({}) CoapClient gatewayClient;
    private @NonNullByDefault({}) TradfriCoapClient gatewayInfoClient;

    private @NonNullByDefault({}) DTLSConnector dtlsConnector;
    private @Nullable CoapEndpoint endPoint;

    private @NonNullByDefault({}) TradfriResourceListObserver deviceListObserver;
    private @NonNullByDefault({}) TradfriResourceListObserver groupListObserver;

    private final @NonNullByDefault({}) Map<String, TradfriCoapResourceProxy<? extends TradfriResource>> proxyMap;

    private final @NonNullByDefault({}) TradfriResourceEventHandler<@NonNull TradfriDevice> discoveryDeviceUpdatedAdapter;
    private final @NonNullByDefault({}) TradfriResourceEventHandler<@NonNull TradfriGroup> discoveryGroupUpdatedAdapter;
    private final @NonNullByDefault({}) TradfriResourceEventHandler<@NonNull TradfriGroup> discoveryGroupRemovedAdapter;

    private final AtomicInteger activeScans = new AtomicInteger(0);

    private @Nullable ScheduledFuture<?> supvJob;

    private int pingLosses = 0;
    private int coapErrors = 0;

    private @Nullable TradfriGatewayData gatewayData;

    public TradfriGatewayHandler(Bridge bridge, TradfriDiscoveryService ds) {
        super(bridge);

        this.proxyMap = new ConcurrentHashMap<String, TradfriCoapResourceProxy<? extends TradfriResource>>();

        this.discoveryDeviceUpdatedAdapter = (data) -> {
            if (mustNotifyDiscoveryService()) {
                String id = data.getInstanceId();
                // TODO inform discovery service only if relevant data changed (like name of device)
                ds.onDeviceUpdate(getThing(), id, gson.toJsonTree(data).getAsJsonObject());
            }
        };

        this.discoveryGroupUpdatedAdapter = (data) -> {
            if (mustNotifyDiscoveryService()) {
                // TODO inform discovery service only if relevant data changed (like name of group)
                ds.onGroupUpdated(getThing(), data);
            }
        };

        this.discoveryGroupRemovedAdapter = (data) -> ds.onGroupRemoved(getThing(), data);
    };

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
                initializeConnection();
                initializeResourceListObserver();

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
                    initializeResourceListObserver();
                }
            });
        }

        updateStatus(ThingStatus.UNKNOWN);
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
        supvJob = scheduler.scheduleWithFixedDelay(this::checkConnection, 1, 1, TimeUnit.MINUTES);

    }

    /**
     * Initialize all observer to requests the lists of available devices and groups.
     * Hint: The native CoAP observe mechanism is currently not supported by the TRADFRI gateway
     * for lists of devices and groups. Therefore the ResourceListObserver are polling
     * the gateway every 60 seconds for changes.
     */
    private void initializeResourceListObserver() {
        // Create observer for devices, groups and scenes and observe lists

        this.deviceListObserver = new TradfriResourceListObserver(getGatewayURI() + "/" + ENDPOINT_DEVICES,
                getEndpoint(), scheduler);
        this.deviceListObserver.registerHandler(this::handleDeviceListChange);

        this.groupListObserver = new TradfriResourceListObserver(getGatewayURI() + "/" + ENDPOINT_GROUPS, getEndpoint(),
                scheduler);
        this.groupListObserver.registerHandler(this::handleGroupListChange);
    }

    private void checkConnection() {
        if (this.gatewayClient != null && getEndpoint() != null) {
            if (this.gatewayClient.ping(TradfriCoapClient.TIMEOUT)) {
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

        // Start observation of devices and groups
        this.deviceListObserver.observe();
        this.groupListObserver.observe();
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

    public void onConnectionError(String detailedError) {
        onConnectionError(detailedError, true);
    }

    public void onConnectionError(String detailedError, boolean resumeConnection) {
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
        // Restart endpoint
        if (this.endPoint != null) {
            try {
                this.endPoint.stop();
                this.endPoint.start();
                this.coapErrors = 0;
                logger.debug("Started CoAP endpoint {}", this.endPoint.getAddress());

                // TODO Invalidate all resource proxies?

            } catch (IOException e) {
                logger.error("Could not start CoAP endpoint", e);
            }
        }
    }

    @Override
    public void dispose() {
        stopScan();

        disposeResourceListObserver();

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

    private void disposeResourceListObserver() {

        if (this.deviceListObserver != null) {
            this.deviceListObserver.dispose();
            this.deviceListObserver = null;
        }
        if (this.groupListObserver != null) {
            this.groupListObserver.dispose();
            this.groupListObserver = null;
        }
    }

    public <T extends TradfriResource> @Nullable TradfriResourceProxy<T> getTradfriResource(String id) {
        return (TradfriResourceProxy<T>) this.proxyMap.get(id);
    }

    /**
     * Enables background discovery of devices, groups and scenes
     */
    public void startBackgroundDiscovery() {
        this.activeScans.getAndIncrement();
    }

    /**
     * Disables background discovery of devices, groups and scenes
     */
    public void stopBackgroundDiscovery() {
        this.activeScans.getAndDecrement();
    }

    /**
     * Forces to scan devices, groups and scenes
     */
    public synchronized void startScan() {
        this.activeScans.getAndIncrement();

        this.deviceListObserver.triggerUpdate();
        this.groupListObserver.triggerUpdate();

        this.proxyMap.forEach((id, proxy) -> proxy.triggerUpdate());
    }

    /**
     * Stops the scan of devices, groups and scenes
     */
    public synchronized void stopScan() {
        this.activeScans.getAndDecrement();
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

    private void updateOnlineStatus() {
        Bridge gateway = getThing();
        ThingStatus status = gateway.getStatus();
        if (status != ThingStatus.ONLINE) {
            boolean hasFwVersion = gateway.getProperties().containsKey(Thing.PROPERTY_FIRMWARE_VERSION);

            boolean observerInitialzed = this.deviceListObserver.isInitialized()
                    && this.groupListObserver.isInitialized();

            if (hasFwVersion && observerInitialzed) {
                updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);
            }
        }
    }

    private synchronized void requestGatewayInfo() {
        gatewayInfoClient.asyncGet(new CoapCallback() {
            @Override
            public void onUpdate(JsonElement data) {
                logger.debug("requestGatewayInfo response: {}", data);
                try {
                    gatewayData = gson.fromJson(data, TradfriGatewayData.class);
                    getThing().setProperty(Thing.PROPERTY_FIRMWARE_VERSION, gatewayData.getVersion());
                    updateOnlineStatus();
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

    private synchronized void handleDeviceListChange(ResourceListEvent event, String id) {
        // A device was added.
        if (event == ResourceListEvent.RESOURCE_ADDED) {
            if (!this.proxyMap.containsKey(id)) {
                CoapEndpoint endpoint = getEndpoint();
                if (endpoint != null) {
                    // Create new proxy for added device
                    TradfriCoapResourceProxy<TradfriDevice> proxy = new TradfriCoapDeviceProxy(getGatewayURI(), id,
                            endpoint, scheduler);
                    // Add this proxy to the list of proxies
                    this.proxyMap.put(id, proxy);
                    // Register handler to update discovery results
                    proxy.registerHandler(discoveryDeviceUpdatedAdapter);
                    // Start observation of device updates
                    proxy.observe();
                } else {
                    logger.error("Unexpected error. No coap endpoint available. Device with ID {} couldn't be added.",
                            id);
                }
            }
            // A device was removed
        } else if (event == ResourceListEvent.RESOURCE_REMOVED) {
            if (this.proxyMap.containsKey(id)) {
                // Remove proxy of removed device
                TradfriCoapResourceProxy<? extends TradfriResource> proxy = this.proxyMap.remove(id);
                // TODO: check if there is a configured thing for that proxy
                // Destroy proxy
                proxy.dispose();
            }
        }

        updateOnlineStatus();
    }

    private synchronized void handleGroupListChange(ResourceListEvent event, String id) {
        // A group was added
        if (event == ResourceListEvent.RESOURCE_ADDED) {
            if (!this.proxyMap.containsKey(id)) {
                CoapEndpoint endpoint = getEndpoint();
                if (endpoint != null) {
                    // Create new proxy for added group
                    TradfriCoapResourceProxy<TradfriGroup> proxy = new TradfriCoapGroupProxy(getGatewayURI(), id,
                            endpoint, scheduler);
                    // Add this proxy to the list of proxies
                    this.proxyMap.put(id, proxy);
                    // Register handler to update discovery results
                    proxy.registerHandler(discoveryGroupUpdatedAdapter);
                    // Start observation of group updates
                    proxy.observe();
                } else {
                    logger.error("Unexpected error. No coap endpoint available. Group with ID {} couldn't be added.",
                            id);
                }
            }
            // A group was removed
        } else if (event == ResourceListEvent.RESOURCE_REMOVED) {
            if (this.proxyMap.containsKey(id)) {
                // Remove proxy of removed group
                TradfriCoapResourceProxy<? extends TradfriResource> proxy = this.proxyMap.remove(id);
                if (mustNotifyDiscoveryService()) {
                    TradfriGroup data = (TradfriGroup) proxy.getData();
                    if (data != null) {
                        this.discoveryGroupRemovedAdapter.onUpdate(data);
                    }
                }
                // TODO: check if there is a configured thing for that proxy
                // Destroy proxy
                proxy.dispose();
            }
        }

        updateOnlineStatus();
    }

    private boolean mustNotifyDiscoveryService() {
        return this.activeScans.get() > 0;
    }

    private boolean isNullOrEmpty(@Nullable String string) {
        return string == null || string.isEmpty();
    }

}
