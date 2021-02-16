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

package org.openhab.binding.tradfri.internal.coap.proxy;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.tradfri.internal.TradfriColor;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapClient;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapResourceCache;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapLight;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapLightSetting;
import org.openhab.binding.tradfri.internal.model.TradfriLight;

import com.google.gson.JsonObject;

/**
 * {@link TradfriCoapLightProxy} represents a single light bulb that
 * - has continuous brightness control and/or
 * - supports different color temperature settings and/or
 * - support full color
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public abstract class TradfriCoapLightProxy extends TradfriCoapDeviceProxy implements TradfriLight {

    protected TradfriCoapLightProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient,
            String coapPath, JsonObject initialData, ThingTypeUID thingType) {
        super(resourceCache, coapClient, coapPath, gson.fromJson(initialData, TradfriCoapLight.class), thingType);
    }

    @Override
    public boolean isOn() {
        return this.getOnOff() > 0;
    }

    @Override
    public boolean isOff() {
        return !this.isOn();
    }

    @Override
    public void setOnOff(OnOffType value) {
        // TODO Auto-generated method stub
    }

    @Override
    public PercentType getBrightness() {
        return isOn() ? TradfriColor.xyBrightnessToPercentType(getDimmer()) : PercentType.ZERO;
    }

    @Override
    public void setBrightness(PercentType value) {
        setBrightness(convertToAbsoluteBrightness(value));
    }

    @Override
    public void increaseBrightnessBy(PercentType value) {
        setBrightness(Math.min(getDimmer() + convertToAbsoluteBrightness(value), 254));
    }

    @Override
    public void decreaseBrightnessBy(PercentType value) {
        setBrightness(Math.max(getDimmer() - convertToAbsoluteBrightness(value), 0));
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
    protected TradfriCoapLight parsePayload(String coapPayload) {
        return gson.fromJson(coapPayload, TradfriCoapLight.class);
    }

    protected int getDimmer() {
        return getLightSetting().map(lightSetting -> lightSetting.getDimmer()).orElse(-1);
    }

    private int getOnOff() {
        return getLightSetting().map(lightSetting -> lightSetting.getOnOff()).orElse(-1);
    }

    private int getColorX() {
        return getLightSetting().map(lightSetting -> lightSetting.getColorX()).orElse(-1);
    }

    private int getColorY() {
        return getLightSetting().map(lightSetting -> lightSetting.getColorY()).orElse(-1);
    }

    private void setBrightness(int value) {
        // TODO implement
    }

    private int convertToAbsoluteBrightness(PercentType relativeBrightness) {
        return (int) Math.floor(relativeBrightness.doubleValue() * 2.54);
    }

    private Optional<TradfriCoapLightSetting> getLightSetting() {
        return getDataAs(TradfriCoapLight.class).flatMap(light -> light.getLightSetting());
    }

}
