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

package org.openhab.binding.tradfri.internal.coap.proxy;

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.THING_TYPE_MOTION_SENSOR;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapClient;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapResourceCache;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapDevice;

/**
 * {@link TradfriCoapMotionSensorProxy} represents the motion sensor capable of reporting the battery level.
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapMotionSensorProxy extends TradfriCoapDeviceProxy {

    public TradfriCoapMotionSensorProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient,
            String coapPath, String coapPayload) {
        super(resourceCache, coapClient, coapPath, dtoFrom(coapPayload, TradfriCoapDevice.class),
                THING_TYPE_MOTION_SENSOR);
    }
}
