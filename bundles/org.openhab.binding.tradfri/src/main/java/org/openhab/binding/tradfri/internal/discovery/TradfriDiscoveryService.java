/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.tradfri.internal.discovery;

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.*;
import static org.openhab.core.thing.Thing.*;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.DeviceUpdateListener;
import org.openhab.binding.tradfri.internal.config.TradfriDeviceConfig;
import org.openhab.binding.tradfri.internal.config.TradfriGroupConfig;
import org.openhab.binding.tradfri.internal.handler.TradfriGatewayHandler;
import org.openhab.binding.tradfri.internal.model.TradfriDevice;
import org.openhab.binding.tradfri.internal.model.TradfriResource;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * This class identifies devices that are available on the gateway and adds discovery results for them.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Christoph Weitkamp - Added support for remote controller and motion sensor devices (read-only battery level)
 * @author Andre Fuechsel - fixed the results removal
 * @author Manuel Raffel - Added support for blinds
 */
@NonNullByDefault
public class TradfriDiscoveryService extends AbstractDiscoveryService
        implements DeviceUpdateListener, DiscoveryService, ThingHandlerService {
    private final Logger logger = LoggerFactory.getLogger(TradfriDiscoveryService.class);

    private @Nullable TradfriGatewayHandler handler;

    public TradfriDiscoveryService() {
        super(DISCOVERABLE_TYPES_UIDS, 10, true);
    }

    @Override
    protected void startScan() {
        handler.startScan();
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan());
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof TradfriGatewayHandler) {
            this.handler = (TradfriGatewayHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return handler;
    }

    @Override
    public void activate() {
        handler.registerDeviceUpdateListener(this);
    }

    @Override
    public void deactivate() {
        removeOlderResults(new Date().getTime());
        handler.unregisterDeviceUpdateListener(this);
    }

    @Override
    public void onUpdate(@Nullable String instanceId, @Nullable JsonObject data) {
        ThingUID bridge = handler.getThing().getUID();
        try {
            if (data != null && data.has(RESOURCE_INSTANCE_ID)) {
                int id = data.get(RESOURCE_INSTANCE_ID).getAsInt();
                int type = data.get(DEVICE_TYPE).getAsInt();
                JsonObject deviceInfo = data.get(DEVICE).getAsJsonObject();
                String model = deviceInfo.get(DEVICE_MODEL).getAsString();
                ThingUID thingId = null;

                if (DEVICE_TYPE_LIGHT == type && data.has(LIGHT)) {
                    JsonObject state = data.get(LIGHT).getAsJsonArray().get(0).getAsJsonObject();

                    // Color temperature light:
                    // We do not always receive a COLOR attribute, even the light supports it - but the gateway does not
                    // seem to have this information, if the bulb is unreachable. We therefore also check against
                    // concrete model names.
                    // Color light:
                    // As the protocol does not distinguishes between color and full-color lights,
                    // we check if the "CWS" or "CW/S" identifier is given in the model name
                    ThingTypeUID thingType = null;
                    if (model != null && Arrays.stream(COLOR_MODEL_IDENTIFIER_HINTS).anyMatch(model::contains)) {
                        thingType = THING_TYPE_COLOR_LIGHT;
                    }
                    if (thingType == null && //
                            (state.has(COLOR) || (model != null && COLOR_TEMP_MODELS.contains(model)))) {
                        thingType = THING_TYPE_COLOR_TEMP_LIGHT;
                    }
                    if (thingType == null) {
                        thingType = THING_TYPE_DIMMABLE_LIGHT;
                    }
                    thingId = new ThingUID(thingType, bridge, Integer.toString(id));
                } else if (DEVICE_TYPE_BLINDS == type && data.has(BLINDS)) {
                    // Blinds
                    thingId = new ThingUID(THING_TYPE_BLINDS, bridge, Integer.toString(id));
                } else if (DEVICE_TYPE_PLUG == type && data.has(PLUG)) {
                    // Smart plug
                    thingId = new ThingUID(THING_TYPE_ONOFF_PLUG, bridge, Integer.toString(id));
                } else if (DEVICE_TYPE_SWITCH == type && data.has(SWITCH)) {
                    // Remote control and wireless dimmer
                    // As protocol does not distinguishes between remote control and wireless dimmer,
                    // we check for the whole model name
                    ThingTypeUID thingType = (model != null && REMOTE_CONTROLLER_MODEL.equals(model))
                            ? THING_TYPE_REMOTE_CONTROL
                            : THING_TYPE_DIMMER;
                    thingId = new ThingUID(thingType, bridge, Integer.toString(id));
                } else if (DEVICE_TYPE_REMOTE == type) {
                    thingId = new ThingUID(THING_TYPE_OPEN_CLOSE_REMOTE_CONTROL, bridge, Integer.toString(id));
                } else if (DEVICE_TYPE_SENSOR == type && data.has(SENSOR)) {
                    // Motion sensor
                    thingId = new ThingUID(THING_TYPE_MOTION_SENSOR, bridge, Integer.toString(id));
                }

                if (thingId == null) {
                    // we didn't identify any device, so let's quit
                    logger.debug("Ignoring unknown device on TRADFRI gateway:\n\ttype : {}\n\tmodel: {}\n\tinfo : {}",
                            type, model, deviceInfo.getAsString());
                    return;
                }

                String label = data.get(RESOURCE_NAME).getAsString();

                Map<String, Object> properties = new HashMap<>(1);
                properties.put("id", id);
                if (model != null) {
                    properties.put(PROPERTY_MODEL_ID, model);
                }
                if (deviceInfo.get(DEVICE_VENDOR) != null) {
                    properties.put(PROPERTY_VENDOR, deviceInfo.get(DEVICE_VENDOR).getAsString());
                }
                if (deviceInfo.get(DEVICE_FIRMWARE) != null) {
                    properties.put(PROPERTY_FIRMWARE_VERSION, deviceInfo.get(DEVICE_FIRMWARE).getAsString());
                }

                logger.debug("Adding device {} to inbox", thingId);
                DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingId).withBridge(bridge)
                        .withLabel(label).withProperties(properties).withRepresentationProperty("id").build();
                thingDiscovered(discoveryResult);
            }
        } catch (JsonSyntaxException e) {
            logger.debug("JSON error during discovery: {}", e.getMessage());
        }
    }

    public void onDeviceUpdated(TradfriDevice device) {
        final ThingUID bridgeUID = handler.getThing().getUID();
        final ThingUID thingId = new ThingUID(device.getThingType(), bridgeUID, device.getInstanceId().orElse("-1"));

        String label = device.getName().orElse("missing device name");

        Map<String, Object> properties = new HashMap<>(1);

        device.getInstanceId().ifPresent(id -> properties.put(TradfriDeviceConfig.CONFIG_ID, Integer.valueOf(id)));
        device.getModel().ifPresent(model -> properties.put(PROPERTY_MODEL_ID, model));
        device.getVendor().ifPresent(vendor -> properties.put(PROPERTY_VENDOR, vendor));
        device.getFirmwareVersion()
                .ifPresent(firmwareVersion -> properties.put(PROPERTY_FIRMWARE_VERSION, firmwareVersion));

        logger.debug("Adding device {} to inbox", thingId);
        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingId).withBridge(bridgeUID).withLabel(label)
                .withProperties(properties).withRepresentationProperty(TradfriDeviceConfig.CONFIG_ID).build();
        thingDiscovered(discoveryResult);
    }

    public void onGroupUpdated(TradfriResource group) {
        final ThingUID bridgeUID = handler.getThing().getUID();
        final ThingUID thingId = new ThingUID(THING_TYPE_GROUP, bridgeUID, group.getInstanceId().orElse("-1"));

        String label = group.getName().orElse("missing group name");

        Map<String, Object> properties = new HashMap<>(1);
        properties.put(PROPERTY_VENDOR, TRADFRI_VENDOR_NAME);
        properties.put(PROPERTY_MODEL_ID, "TRADFRI group of devices");

        group.getInstanceId().ifPresent(id -> properties.put(TradfriGroupConfig.CONFIG_ID, Integer.valueOf(id)));

        logger.debug("Inbox change: adding or updating group {}", thingId);
        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingId).withBridge(bridgeUID).withLabel(label)
                .withProperties(properties).withRepresentationProperty(TradfriGroupConfig.CONFIG_ID).build();
        thingDiscovered(discoveryResult);
    }

    public void onGroupRemoved(TradfriResource group) {
        final ThingUID bridgeUID = handler.getThing().getUID();
        final ThingUID thingId = new ThingUID(THING_TYPE_GROUP, bridgeUID, group.getInstanceId().orElse("-1"));
        logger.debug("Inbox change: removing group {}", thingId);
        thingRemoved(thingId);
    }

}
