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

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.THING_TYPE_DIMMABLE_LIGHT;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.tradfri.internal.TradfriColor;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapDevice;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapDimmableLight;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapDimmableLightSetting;
import org.openhab.binding.tradfri.internal.model.TradfriDimmableLight;

/**
 * {@link TradfriCoapDimmableLightProxy} represents a single light bulb
 * that has continuous brightness control.
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapDimmableLightProxy extends TradfriCoapDeviceProxy implements TradfriDimmableLight {

    private static final ThingTypeUID thingType = THING_TYPE_DIMMABLE_LIGHT;

    public TradfriCoapDimmableLightProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient,
            ScheduledExecutorService scheduler) {
        this(resourceCache, thingType, coapClient, scheduler);
    }

    protected TradfriCoapDimmableLightProxy(TradfriCoapResourceCache resourceCache, ThingTypeUID thingType,
            TradfriCoapClient coapClient, ScheduledExecutorService scheduler) {
        super(resourceCache, thingType, coapClient, scheduler);
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
    public PercentType getBrightness() {
        return isOn() ? TradfriColor.xyBrightnessToPercentType(getDimmer()) : PercentType.ZERO;
    }

    @Override
    public void setBrightness(PercentType value) {
        // TODO Auto-generated method stub
    }

    @Override
    public TradfriCoapDevice parsePayload(String coapPayload) {
        return gson.fromJson(coapPayload, TradfriCoapDimmableLight.class);
    }

    protected int getOnOff() {
        int onOff = -1;
        TradfriCoapDimmableLightSetting lightSetting = getDimmableLightSetting();
        if (lightSetting != null) {
            onOff = lightSetting.getOnOff();
        }
        return onOff;
    }

    protected int getDimmer() {
        int dimmer = -1;
        TradfriCoapDimmableLightSetting lightSetting = getDimmableLightSetting();
        if (lightSetting != null) {
            dimmer = lightSetting.getDimmer();
        }
        return dimmer;
    }

    protected @Nullable TradfriCoapDimmableLightSetting getDimmableLightSetting() {
        Optional<TradfriCoapDimmableLightSetting> lightSetting = getDataAs(TradfriCoapDimmableLight.class)
                .flatMap(light -> light.getLightSetting());
        return lightSetting.isPresent() ? lightSetting.get() : null;
    }

}
