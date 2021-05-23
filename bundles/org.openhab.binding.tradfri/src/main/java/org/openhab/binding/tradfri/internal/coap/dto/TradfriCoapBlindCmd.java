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
package org.openhab.binding.tradfri.internal.coap.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.tradfri.internal.TradfriBindingConstants;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapResourceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonPrimitive;

/**
 * The {@link TradfriCoapBlindCmd} class is used for a data transfer object (DTO). It represents the payload of CoAP
 * commands for lights.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapBlindCmd extends TradfriCoapCmd {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public TradfriCoapBlindCmd(TradfriCoapResourceProxy proxy) {
        super(proxy, TradfriBindingConstants.BLINDS);
    }

    public TradfriCoapBlindCmd setPosition(int value) {
        if (0 <= value && value <= 100) {
            addCommandProperty(TradfriBindingConstants.POSITION, new JsonPrimitive(value));
        } else {
            logger.error("Postion value '{}' out of range for resource {}.", value, getInstanceId());
        }
        return this;
    }

    public TradfriCoapBlindCmd setStop() {
        addCommandProperty(TradfriBindingConstants.STOP_TRIGGER, new JsonPrimitive(0));
        return this;
    }
}
