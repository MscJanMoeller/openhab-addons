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

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.tradfri.internal.TradfriColor;
import org.openhab.binding.tradfri.internal.coap.status.TradfriDevice;
import org.openhab.binding.tradfri.internal.coap.status.TradfriDimmableLight;
import org.openhab.binding.tradfri.internal.coap.status.TradfriDimmableLightSetting;
import org.openhab.binding.tradfri.internal.model.TradfriDimmableLightProxy;

/**
 * {@link TradfriCoapDimmableLightProxy} represents a single light bulb
 * that has continuous brightness control.
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapDimmableLightProxy extends TradfriCoapDeviceProxy implements TradfriDimmableLightProxy {

    private static final ThingTypeUID thingType = THING_TYPE_DIMMABLE_LIGHT;

    public TradfriCoapDimmableLightProxy(TradfriCoapClient coapClient, ScheduledExecutorService scheduler) {
        this(thingType, coapClient, scheduler);
    }

    protected TradfriCoapDimmableLightProxy(ThingTypeUID thingType, TradfriCoapClient coapClient,
            ScheduledExecutorService scheduler) {
        super(thingType, coapClient, scheduler);
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
        return TradfriColor.xyBrightnessToPercentType(getDimmer());
    }

    @Override
    public void setBrightness(PercentType value) {
        // TODO Auto-generated method stub
    }

    @Override
    protected TradfriDevice convert(String coapPayload) {
        return gson.fromJson(coapPayload, TradfriDimmableLight.class);
    }

    protected int getOnOff() {
        int onOff = -1;
        TradfriDimmableLightSetting lightSetting = getDimmableLightSetting();
        if (lightSetting != null) {
            onOff = lightSetting.getOnOff();
        }
        return onOff;
    }

    protected int getDimmer() {
        int dimmer = -1;
        TradfriDimmableLightSetting lightSetting = getDimmableLightSetting();
        if (lightSetting != null) {
            dimmer = lightSetting.getDimmer();
        }
        return dimmer;
    }

    protected @Nullable TradfriDimmableLightSetting getDimmableLightSetting() {
        TradfriDimmableLightSetting lightSetting = null;
        if (this.cachedData != null) {
            lightSetting = ((TradfriDimmableLight) this.cachedData).getLightSetting();
        }
        return lightSetting;
    }

}
