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

import java.util.Optional;
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
    public Optional<PercentType> getColorTemperature() {
        int colorX = getColorX();
        int colorY = getColorY();
        if (colorX > -1 && colorY > -1) {
            TradfriColor color = new TradfriColor(colorX, colorY, null);
            return Optional.of(color.getColorTemperature());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void setColorTemperature(PercentType value) {
        final TradfriColor color = new TradfriColor(value);
        final int x = color.xyX;
        final int y = color.xyY;

        // TODO: create CoapCommand
    }

    @Override
    public void increaseColorTemperatureBy(PercentType value) {
        getColorTemperature().ifPresent(current -> setColorTemperature(
                new PercentType(Math.min(current.intValue() + value.intValue(), PercentType.HUNDRED.intValue()))));
    }

    @Override
    public void decreaseColorTemperatureBy(PercentType value) {
        getColorTemperature().ifPresent(current -> setColorTemperature(
                new PercentType(Math.max(current.intValue() - value.intValue(), PercentType.ZERO.intValue()))));
    }

    @Override
    public Optional<HSBType> getColor() {
        if (isOn()) {
            // XY color coordinates plus brightness is needed for color calculation
            int colorX = getColorX();
            int colorY = getColorY();
            int brightness = getDimmer();
            if (colorX > -1 && colorY > -1 && brightness > -1) {
                TradfriColor color = new TradfriColor(colorX, colorY, brightness);
                return Optional.of(color.getHSB());
            }
        } else {
            Optional.of(HSBType.BLACK);
        }

        return Optional.empty();
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
        return getColorLightSetting();
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
        Optional<TradfriCoapColorLightSetting> lightSetting = getDataAs(TradfriCoapColorLight.class)
                .flatMap(light -> light.getLightSetting());
        return lightSetting.isPresent() ? lightSetting.get() : null;
    }
}
