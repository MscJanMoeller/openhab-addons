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

package org.openhab.binding.tradfri.internal.coap;

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.*;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * The {@link TradfriThingTypeMap} determines the thing type from a given
 * CoAP response of a Tradfri device.
 *
 * @author Jan Möller - Initial contribution (based on TradfriDiscoveryService)
 *
 */

@NonNullByDefault
public class TradfriThingTypeMap {

    private static final Logger logger = LoggerFactory.getLogger(TradfriThingTypeMap.class);

    public static @Nullable ThingTypeUID getThingTypeFrom(JsonObject devicePayload) {
        ThingTypeUID thingType = null;
        try {
            if (devicePayload.has(RESOURCE_INSTANCE_ID)) {
                int type = devicePayload.get(DEVICE_TYPE).getAsInt();
                JsonObject deviceInfo = devicePayload.get(DEVICE).getAsJsonObject();
                String model = deviceInfo.get(DEVICE_MODEL).getAsString();

                if (DEVICE_TYPE_LIGHT == type && devicePayload.has(LIGHT)) {
                    JsonObject state = devicePayload.get(LIGHT).getAsJsonArray().get(0).getAsJsonObject();

                    // Color temperature light:
                    // We do not always receive a COLOR attribute, even the light supports it - but the gateway does not
                    // seem to have this information, if the bulb is unreachable. We therefore also check against
                    // concrete model names.
                    // Color light:
                    // As the protocol does not distinguishes between color and full-color lights,
                    // we check if the "CWS" or "CW/S" identifier is given in the model name
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
                } else if (DEVICE_TYPE_BLINDS == type && devicePayload.has(BLINDS)) {
                    // Blinds
                    thingType = THING_TYPE_BLINDS;
                } else if (DEVICE_TYPE_PLUG == type && devicePayload.has(PLUG)) {
                    // Smart plug
                    thingType = THING_TYPE_ONOFF_PLUG;
                } else if (DEVICE_TYPE_SWITCH == type && devicePayload.has(SWITCH)) {
                    // Remote control and wireless dimmer
                    // As protocol does not distinguishes between remote control and wireless dimmer,
                    // we check for the whole model name
                    thingType = (model != null && REMOTE_CONTROLLER_MODEL.equals(model)) ? THING_TYPE_REMOTE_CONTROL
                            : THING_TYPE_DIMMER;
                } else if (DEVICE_TYPE_REMOTE == type) {
                    thingType = THING_TYPE_OPEN_CLOSE_REMOTE_CONTROL;
                } else if (DEVICE_TYPE_SENSOR == type && devicePayload.has(SENSOR)) {
                    // Motion sensor
                    thingType = THING_TYPE_MOTION_SENSOR;
                }
            }
        } catch (IllegalStateException e) {
            logger.error("JSON error during mapping CoAP device payload to thing type: {}", e.getMessage());
        }

        return thingType;
    }
}
