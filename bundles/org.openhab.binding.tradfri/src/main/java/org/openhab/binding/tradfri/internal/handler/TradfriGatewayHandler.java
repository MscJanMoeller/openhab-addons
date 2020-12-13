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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
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
import org.openhab.binding.tradfri.internal.coap.TradfriCoapProxyFactory;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapResourceProxy;
import org.openhab.binding.tradfri.internal.coap.TradfriResourceListEventHandler.ResourceListEvent;
import org.openhab.binding.tradfri.internal.coap.TradfriResourceListObserver;
import org.openhab.binding.tradfri.internal.coap.status.TradfriGateway;
import org.openhab.binding.tradfri.internal.config.TradfriGatewayConfig;
import org.openhab.binding.tradfri.internal.discovery.TradfriDiscoveryService;
import org.openhab.binding.tradfri.internal.model.TradfriDeviceProxy;
import org.openhab.binding.tradfri.internal.model.TradfriResourceEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriResourceProxy;
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

    private static final Gson gson = new Gson();

    private @Nullable String gatewayURI;

    private @Nullable CoapClient gatewayClient;
    private @Nullable TradfriCoapClient gatewayInfoClient;

    private @Nullable Endpoint endpoint;

    private @Nullable TradfriResourceListObserver deviceListObserver;
    private @Nullable TradfriResourceListObserver groupListObserver;

    private final ConcurrentHashMap<String, @Nullable Set<TradfriResourceEventHandler>> resourceUpdateHandler;

    private @Nullable TradfriCoapProxyFactory proxyFactory;

    private final ConcurrentHashMap<String, TradfriCoapResourceProxy> proxyMap;

    private final TradfriResourceEventHandler discoveryDeviceUpdatedAdapter;
    private final TradfriResourceEventHandler discoveryGroupUpdatedAdapter;
    private final TradfriResourceEventHandler discoveryGroupRemovedAdapter;

    private final AtomicInteger activeScans = new AtomicInteger(0);

    private @Nullable ScheduledFuture<?> supvJob;

    private int pingLosses = 0;
    private int coapErrors = 0;

    public TradfriGatewayHandler(Bridge bridge, TradfriDiscoveryService ds) {
        super(bridge);

        this.resourceUpdateHandler = new ConcurrentHashMap<String, @Nullable Set<TradfriResourceEventHandler>>();

        this.proxyMap = new ConcurrentHashMap<String, TradfriCoapResourceProxy>();

        this.discoveryDeviceUpdatedAdapter = (proxy) -> {
            if (mustNotifyDiscoveryService()) {
                // TODO inform discovery service only if relevant data changed (like name of device)
                if (proxy instanceof TradfriDeviceProxy) {
                    ds.onDeviceUpdated(getThing(), (TradfriDeviceProxy) proxy);
                }
            }
        };

        this.discoveryGroupUpdatedAdapter = (proxy) -> {
            if (mustNotifyDiscoveryService()) {
                // TODO inform discovery service only if relevant data changed (like name of group)
                ds.onGroupUpdated(getThing(), proxy);
            }
        };

        this.discoveryGroupRemovedAdapter = (proxy) -> ds.onGroupRemoved(getThing(), proxy);
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

        String baseUri = getGatewayURI();
        if (baseUri != null) {
            this.proxyFactory = new TradfriCoapProxyFactory(baseUri, endpoint, scheduler);
        }

        if (this.gatewayClient != null) {
            this.gatewayClient.setEndpoint(getEndpoint());
        }

        if (this.gatewayInfoClient != null) {
            this.gatewayInfoClient.setEndpoint(getEndpoint());
        }

        // Schedule a CoAP ping every minute to check the connection
        supvJob = scheduler.scheduleWithFixedDelay(this::checkConnection, 0, 1, TimeUnit.MINUTES);
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
        CoapClient gatewayClient = this.gatewayClient;
        if (gatewayClient != null) {
            if (gatewayClient.ping(TradfriCoapClient.TIMEOUT)) {
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
        if (this.deviceListObserver != null) {
            this.deviceListObserver.observe();
        }
        if (this.groupListObserver != null) {
            this.groupListObserver.observe();
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

        // Implicitly disposes all resources by triggering RESOURCE_REMOVED event
        disposeResourceListObserver();

        for (Set<TradfriResourceEventHandler> handlers : this.resourceUpdateHandler.values()) {
            if (handlers != null) {
                handlers.clear();
            }
        }
        this.resourceUpdateHandler.clear();

        if (this.supvJob != null) {
            this.supvJob.cancel(true);
            this.supvJob = null;
        }
        if (this.endpoint != null) {
            this.endpoint.destroy();
            this.endpoint = null;
        }
        if (this.gatewayClient != null) {
            this.gatewayClient.shutdown();
            this.gatewayClient = null;
        }
        if (this.gatewayInfoClient != null) {
            this.gatewayInfoClient.shutdown();
            this.gatewayInfoClient = null;
        }

        super.dispose();
    }

    private void disposeResourceListObserver() {

        if (this.deviceListObserver != null) {
            // Triggers RESOURCE_REMOVED event for all resources
            this.deviceListObserver.dispose();
            this.deviceListObserver = null;
        }
        if (this.groupListObserver != null) {
            // Triggers RESOURCE_REMOVED event for all resources
            this.groupListObserver.dispose();
            this.groupListObserver = null;
        }
    }

    public @Nullable TradfriResourceProxy getTradfriResource(String id) {
        return this.proxyMap.get(id);
    }

    /**
     * Registers a handler, which will be informed about Tradfri resource updates.
     *
     * @param handler the handler to register
     */
    public void registerResourceUpdateHandler(String id, TradfriResourceEventHandler handler) {
        Set<TradfriResourceEventHandler> handlers = this.resourceUpdateHandler.get(id);
        if (handlers == null) {
            handlers = new CopyOnWriteArraySet<>();
        }
        handlers.add(handler);

        TradfriResourceProxy proxy = getTradfriResource(id);
        if (proxy != null) {
            proxy.unregisterHandler(handler);
        }
    }

    /**
     * Unregisters a given resource update handler.
     *
     * @param handler the handler to unregister
     */
    public void unregisterResourceUpdateHandler(String id, TradfriResourceEventHandler handler) {
        Set<TradfriResourceEventHandler> handlers = this.resourceUpdateHandler.get(id);
        if (handlers != null) {
            handlers.remove(handler);
        }

        TradfriResourceProxy proxy = getTradfriResource(id);
        if (proxy != null) {
            proxy.unregisterHandler(handler);
        }
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

        if (this.deviceListObserver != null) {
            this.deviceListObserver.triggerUpdate();
        }
        if (this.groupListObserver != null) {
            this.groupListObserver.triggerUpdate();
        }

        this.proxyMap.values().parallelStream().forEach((proxy) -> proxy.triggerUpdate());
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
    public @Nullable String getGatewayURI() {
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

    private void updateOnlineStatus() {
        Bridge gateway = getThing();
        ThingStatus status = gateway.getStatus();
        if (status != ThingStatus.ONLINE) {
            boolean hasFwVersion = gateway.getProperties().containsKey(Thing.PROPERTY_FIRMWARE_VERSION);

            TradfriResourceListObserver deviceListObserver = this.deviceListObserver;
            TradfriResourceListObserver groupListObserver = this.groupListObserver;
            boolean observerInitialzed = false;
            if (deviceListObserver != null && groupListObserver != null) {
                observerInitialzed = deviceListObserver.isInitialized() && groupListObserver.isInitialized();
            }
            if (hasFwVersion && observerInitialzed) {
                updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);
            }
        }
    }

    private void requestGatewayInfo() {
        if (gatewayInfoClient != null) {
            gatewayInfoClient.get(new CoapHandler() {
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
                            TradfriGateway gateway = gson.fromJson(response.getResponseText(), TradfriGateway.class);
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

    private synchronized void handleDeviceListChange(ResourceListEvent event, String id) {
        // A device was added.
        if (event == ResourceListEvent.RESOURCE_ADDED) {
            if (!this.proxyMap.containsKey(id)) {
                if (this.proxyFactory != null) {
                    // Create new proxy for added device
                    this.proxyFactory.createDeviceProxy(id, (proxy) -> {
                        TradfriCoapResourceProxy coapProxy = (TradfriCoapResourceProxy) proxy;
                        // Add this proxy to the list of proxies
                        proxyMap.put(id, coapProxy);
                        // Register handler to update discovery results
                        coapProxy.registerHandler(discoveryDeviceUpdatedAdapter);
                        // Register update handlers of already configured thing
                        Set<TradfriResourceEventHandler> handlers = this.resourceUpdateHandler.get(id);
                        if (handlers != null) {
                            handlers.forEach(handler -> coapProxy.registerHandler(handler));
                        }
                        // Start observation of device updates
                        coapProxy.observe();
                    });
                } else {
                    logger.error("Unexpected error. No coap endpoint available. Device with ID {} couldn't be added.",
                            id);
                }
            }
            // A device was removed
        } else if (event == ResourceListEvent.RESOURCE_REMOVED) {
            if (this.proxyMap.containsKey(id)) {
                // Remove proxy of removed device
                TradfriCoapResourceProxy proxy = this.proxyMap.remove(id);
                // Unregister update handlers of configured thing
                Set<TradfriResourceEventHandler> handlers = this.resourceUpdateHandler.get(id);
                if (handlers != null) {
                    handlers.forEach(handler -> proxy.unregisterHandler(handler));
                }
                // TODO: error handling if there is a configured thing for that proxy
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
                if (this.proxyFactory != null) {
                    // Create new proxy for added group
                    TradfriCoapResourceProxy proxy = this.proxyFactory.createGroupProxy(id);
                    // Add this proxy to the list of proxies
                    this.proxyMap.put(id, proxy);
                    // Register handler to update discovery results
                    proxy.registerHandler(discoveryGroupUpdatedAdapter);
                    // Register update handlers of already configured thing
                    Set<TradfriResourceEventHandler> handlers = this.resourceUpdateHandler.get(id);
                    if (handlers != null) {
                        handlers.forEach(handler -> proxy.registerHandler(handler));
                    }
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
                TradfriCoapResourceProxy proxy = this.proxyMap.remove(id);
                if (mustNotifyDiscoveryService()) {
                    this.discoveryGroupRemovedAdapter.onUpdate(proxy);
                }
                // Unregister update handlers of configured thing
                Set<TradfriResourceEventHandler> handlers = this.resourceUpdateHandler.get(id);
                if (handlers != null) {
                    handlers.forEach(handler -> proxy.unregisterHandler(handler));
                }
                // TODO: error handling if there is a configured thing for that proxy
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
