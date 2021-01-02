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

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.THING_TYPE_COLOR_TEMP_LIGHT;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.tradfri.internal.TradfriColor;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapColorTempLight;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapColorTempLightSetting;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapDevice;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapDimmableLightSetting;
import org.openhab.binding.tradfri.internal.model.TradfriColorTempLight;

/**
 * {@link TradfriCoapColorTempLightProxy} represents a single light bulb
 * that supports different color temperature settings.
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapColorTempLightProxy extends TradfriCoapDimmableLightProxy
        implements TradfriColorTempLight {

    private static final ThingTypeUID thingType = THING_TYPE_COLOR_TEMP_LIGHT;

    public TradfriCoapColorTempLightProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient,
            ScheduledExecutorService scheduler) {
        super(resourceCache, thingType, coapClient, scheduler);
    }

    @Override
    public @Nullable PercentType getColorTemperature() {
        int colorX = getColorX();
        int colorY = getColorY();
        if (colorX > -1 && colorY > -1) {
            TradfriColor color = new TradfriColor(colorX, colorY, null);
            return color.getColorTemperature();
        } else {
            return null;
        }
    }

    @Override
    public void setColorTemperature(PercentType value) {
        // TODO Auto-generated method stub

    }

    @Override
    public TradfriCoapDevice parsePayload(String coapPayload) {
        return gson.fromJson(coapPayload, TradfriCoapColorTempLight.class);
    }

    @Override
    protected @Nullable TradfriCoapDimmableLightSetting getDimmableLightSetting() {
        TradfriCoapDimmableLightSetting lightSetting = null;
        if (this.cachedData != null) {
            lightSetting = ((TradfriCoapColorTempLight) this.cachedData).getLightSetting();
        }
        return lightSetting;
    }

    private int getColorX() {
        int colorX = -1;
        TradfriCoapColorTempLightSetting lightSetting = getColorTempLightSetting();
        if (lightSetting != null) {
            colorX = lightSetting.getColorX();
        }
        return colorX;
    }

    private int getColorY() {
        int colorY = -1;
        TradfriCoapColorTempLightSetting lightSetting = getColorTempLightSetting();
        if (lightSetting != null) {
            colorY = lightSetting.getColorY();
        }
        return colorY;
    }

    private @Nullable TradfriCoapColorTempLightSetting getColorTempLightSetting() {
        TradfriCoapColorTempLightSetting lightSetting = null;
        if (this.cachedData != null) {
            lightSetting = ((TradfriCoapColorTempLight) this.cachedData).getLightSetting();
        }
        return lightSetting;
    }
}
