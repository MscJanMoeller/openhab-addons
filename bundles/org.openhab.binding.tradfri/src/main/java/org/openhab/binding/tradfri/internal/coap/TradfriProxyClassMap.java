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

package org.openhab.binding.tradfri.internal.coap;

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.*;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapColorLightProxy;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapColorTempLightProxy;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapDeviceProxy;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapDimmableLightProxy;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapDimmerProxy;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapGroupProxy;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapMotionSensorProxy;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapRemoteControlOpenCloseProxy;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapRemoteControlProxy;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapResourceProxy;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapSceneProxy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link TradfriProxyClassMap} determines the thing type from a given
 * CoAP response of a Tradfri device.
 *
 * @author Jan Möller - Initial contribution (based on TradfriDiscoveryService)
 *
 */

@NonNullByDefault
public class TradfriProxyClassMap {

    public static @Nullable Class<? extends TradfriCoapResourceProxy> getProxyClassFrom(String coapPayload)
            throws JsonSyntaxException {
        final JsonObject json = JsonParser.parseString(coapPayload).getAsJsonObject();

        Class<? extends TradfriCoapResourceProxy> proxyClass = null;
        if (json.has(RESOURCE_INSTANCE_ID)) {
            if (json.has(DEVICE_TYPE)) {
                proxyClass = getDeviceProxyClassFrom(json);
            } else if (json.has(GROUP_TYPE)) {
                proxyClass = TradfriCoapGroupProxy.class;
            } else if (json.has(SCENE_LIGHT_SETTING)) {
                proxyClass = TradfriCoapSceneProxy.class;
            }
        }

        return proxyClass;
    }

    private static @Nullable Class<? extends TradfriCoapDeviceProxy> getDeviceProxyClassFrom(JsonObject payload) {
        Class<? extends TradfriCoapDeviceProxy> proxyClass = null;

        int type = payload.get(DEVICE_TYPE).getAsInt();
        JsonObject deviceInfo = payload.get(DEVICE).getAsJsonObject();
        String model = deviceInfo.get(DEVICE_MODEL).getAsString();

        if (DEVICE_TYPE_LIGHT == type && payload.has(LIGHT)) {
            JsonObject state = payload.get(LIGHT).getAsJsonArray().get(0).getAsJsonObject();

            // Color temperature light:
            // We do not always receive a COLOR attribute, even the light supports it - but the gateway does not
            // seem to have this information, if the bulb is unreachable. We therefore also check against
            // concrete model names.
            // Color light:
            // As the protocol does not distinguishes between color and full-color lights,
            // we check if the "CWS" or "CW/S" identifier is given in the model name
            if (Arrays.stream(COLOR_MODEL_IDENTIFIER_HINTS).anyMatch(model::contains)) {
                proxyClass = TradfriCoapColorLightProxy.class;
            }
            if (proxyClass == null && //
                    (state.has(COLOR) || (COLOR_TEMP_MODELS.contains(model)))) {
                proxyClass = TradfriCoapColorTempLightProxy.class;
            }
            if (proxyClass == null) {
                proxyClass = TradfriCoapDimmableLightProxy.class;
            }
        } else if (DEVICE_TYPE_BLINDS == type && payload.has(BLINDS)) {
            // Blinds: THING_TYPE_BLINDS
            // TODO: change to specific proxy class
            proxyClass = TradfriCoapDeviceProxy.class;
        } else if (DEVICE_TYPE_PLUG == type && payload.has(PLUG)) {
            // Smart plug: THING_TYPE_ONOFF_PLUG
            // TODO: change to specific proxy class
            proxyClass = TradfriCoapDeviceProxy.class;
        } else if (DEVICE_TYPE_SWITCH == type && payload.has(SWITCH)) {
            // Remote control and wireless dimmer: THING_TYPE_REMOTE_CONTROL, THING_TYPE_DIMMER
            // As protocol does not distinguishes between remote control and wireless dimmer,
            // we check for the whole model name
            proxyClass = (REMOTE_CONTROLLER_MODEL.equals(model)) ? TradfriCoapRemoteControlProxy.class
                    : TradfriCoapDimmerProxy.class;
        } else if (DEVICE_TYPE_REMOTE == type) {
            // THING_TYPE_OPEN_CLOSE_REMOTE_CONTROL
            proxyClass = TradfriCoapRemoteControlOpenCloseProxy.class;
        } else if (DEVICE_TYPE_SENSOR == type && payload.has(SENSOR)) {
            // Motion sensor: THING_TYPE_MOTION_SENSOR
            proxyClass = TradfriCoapMotionSensorProxy.class;
        }

        return proxyClass;
    }
}
