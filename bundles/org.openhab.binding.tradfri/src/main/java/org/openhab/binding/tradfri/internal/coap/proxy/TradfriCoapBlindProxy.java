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

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.THING_TYPE_BLINDS;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapClient;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapResourceCache;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapBlind;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapBlindCmd;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapBlindSetting;
import org.openhab.binding.tradfri.internal.model.TradfriBlind;
import org.openhab.core.library.types.PercentType;

/**
 * {@link TradfriCoapBlindProxy} represents a blind or curtain that can be moved up and down.
 * Also reports current battery level.
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapBlindProxy extends TradfriCoapDeviceProxy implements TradfriBlind {

    public TradfriCoapBlindProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient, String coapPath,
            String coapPayload) {
        super(resourceCache, coapClient, coapPath, dtoFrom(coapPayload, TradfriCoapBlind.class), THING_TYPE_BLINDS);
    }

    @Override
    public PercentType getPosition() {
        int percent = Math.round(getBlindSetting().map(blindSetting -> blindSetting.getPosition()).orElse(-1.0f));
        // Ensure value is within range 0 .. 100
        return new PercentType(Math.min(100, Math.max(percent, 0)));
    }

    @Override
    public void setPosition(PercentType value) {
        execute(new TradfriCoapBlindCmd(this).setPosition(value.intValue()));
    }

    @Override
    public void stop() {
        execute(new TradfriCoapBlindCmd(this).setStop());
    }

    @Override
    protected TradfriCoapBlind parsePayload(String coapPayload) {
        return dtoFrom(coapPayload, TradfriCoapBlind.class);
    }

    private Optional<TradfriCoapBlindSetting> getBlindSetting() {
        return getDataAs(TradfriCoapBlind.class).flatMap(blind -> blind.getBlindSetting());
    }
}
