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

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.THING_TYPE_COLOR_LIGHT;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.tradfri.internal.TradfriColor;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapColorLight;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapColorLightSetting;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapDevice;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapDimmableLightSetting;
import org.openhab.binding.tradfri.internal.model.TradfriColorLight;

/**
 * {@link TradfriCoapColorLightProxy} represents a single light bulb
 * that supports full colors and color temperature settings.
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapColorLightProxy extends TradfriCoapDimmableLightProxy implements TradfriColorLight {

    private static final ThingTypeUID thingType = THING_TYPE_COLOR_LIGHT;

    public TradfriCoapColorLightProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient,
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
    public @Nullable HSBType getColor() {
        // XY color coordinates plus brightness is needed for color calculation
        int colorX = getColorX();
        int colorY = getColorY();
        int brightness = getDimmer();
        if (colorX > -1 && colorY > -1 && brightness > -1) {
            TradfriColor color = new TradfriColor(colorX, colorY, brightness);
            return color.getHSB();
        }
        return null;
    }

    @Override
    public void setColor(HSBType value) {
        // TODO Auto-generated method stub

    }

    @Override
    public TradfriCoapDevice parsePayload(String coapPayload) {
        return gson.fromJson(coapPayload, TradfriCoapColorLight.class);
    }

    @Override
    protected @Nullable TradfriCoapDimmableLightSetting getDimmableLightSetting() {
        TradfriCoapDimmableLightSetting lightSetting = null;
        if (this.cachedData != null) {
            lightSetting = ((TradfriCoapColorLight) this.cachedData).getLightSetting();
        }
        return lightSetting;
    }

    private int getHue() {
        int hue = -1;
        TradfriCoapColorLightSetting lightSetting = getColorLightSetting();
        if (lightSetting != null) {
            hue = lightSetting.getHue();
        }
        return hue;
    }

    private int getSaturation() {
        int saturation = -1;
        TradfriCoapColorLightSetting lightSetting = getColorLightSetting();
        if (lightSetting != null) {
            saturation = lightSetting.getSaturation();
        }
        return saturation;
    }

    private int getColorX() {
        int colorX = -1;
        TradfriCoapColorLightSetting lightSetting = getColorLightSetting();
        if (lightSetting != null) {
            colorX = lightSetting.getColorX();
        }
        return colorX;
    }

    private int getColorY() {
        int colorY = -1;
        TradfriCoapColorLightSetting lightSetting = getColorLightSetting();
        if (lightSetting != null) {
            colorY = lightSetting.getColorY();
        }
        return colorY;
    }

    private @Nullable TradfriCoapColorLightSetting getColorLightSetting() {
        TradfriCoapColorLightSetting lightSetting = null;
        if (this.cachedData != null) {
            lightSetting = ((TradfriCoapColorLight) this.cachedData).getLightSetting();
        }
        return lightSetting;
    }
}
