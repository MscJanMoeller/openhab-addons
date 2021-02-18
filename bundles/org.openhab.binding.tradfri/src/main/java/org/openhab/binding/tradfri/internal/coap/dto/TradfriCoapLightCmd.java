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
package org.openhab.binding.tradfri.internal.coap.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.tradfri.internal.TradfriBindingConstants;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapResourceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonPrimitive;

/**
 * The {@link TradfriCoapLightCmd} class is used for a data transfer object (DTO). It represents the payload of CoAP
 * commands for lights.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapLightCmd extends TradfriCoapCmd {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public TradfriCoapLightCmd(TradfriCoapResourceProxy proxy) {
        super(proxy, TradfriBindingConstants.LIGHT);
    }

    public TradfriCoapLightCmd setOnOff(int value) {
        if (0 <= value && value <= 1) {
            addCommandProperty(TradfriBindingConstants.ONOFF, new JsonPrimitive(value));
        } else {
            logger.error("On/Off value '{}' out of range for resource {}.", value, getInstanceId());
        }
        return this;
    }

    public TradfriCoapLightCmd setDimmer(int value) {
        if (0 <= value && value <= 255) {
            addCommandProperty(TradfriBindingConstants.DIMMER, new JsonPrimitive(value));
        } else {
            logger.error("Dimmer value '{}' out of range for resource {}.", value, getInstanceId());
        }
        return this;
    }

    public TradfriCoapLightCmd setColorXY(int x, int y) {
        if (0 <= x && x <= 65535) {
            addCommandProperty(TradfriBindingConstants.COLOR_X, new JsonPrimitive(x));
        } else {
            logger.error("Color X value '{}' out of range for resource {}.", x, getInstanceId());
        }

        if (0 <= y && y <= 65535) {
            addCommandProperty(TradfriBindingConstants.COLOR_Y, new JsonPrimitive(y));
        } else {
            logger.error("Color Y value '{}' out of range for resource {}.", y, getInstanceId());
        }
        return this;
    }

}
