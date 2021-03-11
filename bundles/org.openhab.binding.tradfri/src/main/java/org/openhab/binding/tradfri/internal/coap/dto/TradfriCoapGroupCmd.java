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
 * The {@link TradfriCoapGroupCmd} class is used for a data transfer object (DTO). It represents the payload of CoAP
 * commands for groups.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapGroupCmd extends TradfriCoapCmd {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public TradfriCoapGroupCmd(TradfriCoapResourceProxy proxy) {
        super(proxy);
    }

    public TradfriCoapGroupCmd setOnOff(int value) {
        if (0 <= value && value <= 1) {
            addCommandProperty(TradfriBindingConstants.ONOFF, new JsonPrimitive(value));
        } else {
            logger.error("On/Off value '{}' out of range for resource {}.", value, getInstanceId());
        }
        return this;
    }

    public TradfriCoapGroupCmd setDimmer(int value) {
        if (0 <= value && value < 255) {
            addCommandProperty(TradfriBindingConstants.DIMMER, new JsonPrimitive(value));
        } else {
            logger.error("Dimmer value '{}' out of range for resource {}.", value, getInstanceId());
        }
        return this;
    }

    public TradfriCoapGroupCmd setScene(String sceneID) {
        addCommandProperty(TradfriBindingConstants.SCENE_ID, new JsonPrimitive(Integer.parseInt(sceneID)));
        return this;
    }

}
