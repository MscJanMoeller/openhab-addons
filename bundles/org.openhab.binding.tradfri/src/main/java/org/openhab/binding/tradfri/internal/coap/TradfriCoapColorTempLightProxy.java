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

import java.util.Optional;
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

import com.google.gson.JsonObject;

/**
 * {@link TradfriCoapColorTempLightProxy} represents a single light bulb
 * that supports different color temperature settings.
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapColorTempLightProxy extends TradfriCoapDimmableLightProxy implements TradfriColorTempLight {

    private static final ThingTypeUID thingType = THING_TYPE_COLOR_TEMP_LIGHT;

    public TradfriCoapColorTempLightProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient,
            ScheduledExecutorService scheduler, JsonObject coapPayload) {
        super(resourceCache, thingType, coapClient, scheduler,
                gson.fromJson(coapPayload, TradfriCoapColorTempLight.class));
    }

    @Override
    public Optional<PercentType> getColorTemperature() {
        final int colorX = getColorX();
        final int colorY = getColorY();
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
    protected TradfriCoapDevice parsePayload(String coapPayload) {
        return gson.fromJson(coapPayload, TradfriCoapColorTempLight.class);
    }

    @Override
    protected @Nullable TradfriCoapDimmableLightSetting getDimmableLightSetting() {
        return getColorTempLightSetting();
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
        Optional<TradfriCoapColorTempLightSetting> lightSetting = getDataAs(TradfriCoapColorTempLight.class)
                .flatMap(light -> light.getLightSetting());
        return lightSetting.isPresent() ? lightSetting.get() : null;
    }
}
