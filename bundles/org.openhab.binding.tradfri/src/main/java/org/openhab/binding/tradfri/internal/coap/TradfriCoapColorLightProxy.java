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
import org.openhab.binding.tradfri.internal.coap.status.TradfriColorLight;
import org.openhab.binding.tradfri.internal.coap.status.TradfriColorLightSetting;
import org.openhab.binding.tradfri.internal.coap.status.TradfriDevice;
import org.openhab.binding.tradfri.internal.coap.status.TradfriDimmableLightSetting;
import org.openhab.binding.tradfri.internal.model.TradfriColorLightProxy;

/**
 * {@link TradfriCoapColorLightProxy} represents a single light bulb
 * that supports full colors and color temperature settings.
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapColorLightProxy extends TradfriCoapDimmableLightProxy implements TradfriColorLightProxy {

    private static final ThingTypeUID thingType = THING_TYPE_COLOR_LIGHT;

    public TradfriCoapColorLightProxy(TradfriCoapClient coapClient, ScheduledExecutorService scheduler) {
        super(thingType, coapClient, scheduler);
    }

    protected TradfriCoapColorLightProxy(ThingTypeUID thingType, TradfriCoapClient coapClient,
            ScheduledExecutorService scheduler) {
        super(thingType, coapClient, scheduler);
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
    protected TradfriDevice convert(String coapPayload) {
        return gson.fromJson(coapPayload, TradfriColorLight.class);
    }

    @Override
    protected @Nullable TradfriDimmableLightSetting getDimmableLightSetting() {
        TradfriDimmableLightSetting lightSetting = null;
        if (this.cachedData != null) {
            lightSetting = ((TradfriColorLight) this.cachedData).getLightSetting();
        }
        return lightSetting;
    }

    private int getHue() {
        int hue = -1;
        TradfriColorLightSetting lightSetting = getColorLightSetting();
        if (lightSetting != null) {
            hue = lightSetting.getHue();
        }
        return hue;
    }

    private int getSaturation() {
        int saturation = -1;
        TradfriColorLightSetting lightSetting = getColorLightSetting();
        if (lightSetting != null) {
            saturation = lightSetting.getSaturation();
        }
        return saturation;
    }

    private int getColorX() {
        int colorX = -1;
        TradfriColorLightSetting lightSetting = getColorLightSetting();
        if (lightSetting != null) {
            colorX = lightSetting.getColorX();
        }
        return colorX;
    }

    private int getColorY() {
        int colorY = -1;
        TradfriColorLightSetting lightSetting = getColorLightSetting();
        if (lightSetting != null) {
            colorY = lightSetting.getColorY();
        }
        return colorY;
    }

    private @Nullable TradfriColorLightSetting getColorLightSetting() {
        TradfriColorLightSetting lightSetting = null;
        if (this.cachedData != null) {
            lightSetting = ((TradfriColorLight) this.cachedData).getLightSetting();
        }
        return lightSetting;
    }
}
