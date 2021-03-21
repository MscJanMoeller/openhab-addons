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

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.THING_TYPE_COLOR_TEMP_LIGHT;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapClient;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapResourceCache;

/**
 * {@link TradfriCoapColorTempLightProxy} represents a single light bulb that:
 * - has continuous brightness control and
 * - supports different color temperature settings
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapColorTempLightProxy extends TradfriCoapLightProxy {

    public TradfriCoapColorTempLightProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient,
            String coapPath, String coapPayload) {
        super(resourceCache, coapClient, coapPath, coapPayload, THING_TYPE_COLOR_TEMP_LIGHT);
    }

    @Override
    public boolean supportsColorTemperature() {
        return true;
    }

    @Override
    public boolean supportsColor() {
        return false;
    }
}
