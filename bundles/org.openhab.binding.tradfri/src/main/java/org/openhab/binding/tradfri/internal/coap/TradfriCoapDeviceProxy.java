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

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapDevice;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapDeviceInfo;
import org.openhab.binding.tradfri.internal.model.TradfriDevice;

/**
 * {@link TradfriCoapDeviceProxy} observes changes of a single device
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapDeviceProxy extends TradfriCoapResourceProxy implements TradfriDevice {

    private final ThingTypeUID thingType;

    protected TradfriCoapDeviceProxy(TradfriCoapResourceCache resourceCache, ThingTypeUID thingType,
            TradfriCoapClient coapClient, ScheduledExecutorService scheduler) {
        super(resourceCache, coapClient, scheduler);
        this.thingType = thingType;
    }

    @Override
    public ThingTypeUID getThingType() {
        return this.thingType;
    }

    @Override
    public boolean matches(ThingTypeUID thingType) {
        return this.thingType.equals(thingType);
    }

    @Override
    public @Nullable String getVendor() {
        String vendor = null;
        if (this.cachedData != null) {
            TradfriCoapDeviceInfo deviceInfo = ((TradfriCoapDevice) this.cachedData).getDeviceInfo();
            if (deviceInfo != null) {
                vendor = deviceInfo.getVendor();
            }
        }
        return vendor;
    }

    @Override
    public @Nullable String getModel() {
        String model = null;
        if (this.cachedData != null) {
            TradfriCoapDeviceInfo deviceInfo = ((TradfriCoapDevice) this.cachedData).getDeviceInfo();
            if (deviceInfo != null) {
                model = deviceInfo.getModel();
            }
        }
        return model;
    }

    @Override
    public @Nullable String getSerialNumber() {
        String serialNumber = null;
        if (this.cachedData != null) {
            TradfriCoapDeviceInfo deviceInfo = ((TradfriCoapDevice) this.cachedData).getDeviceInfo();
            if (deviceInfo != null) {
                serialNumber = deviceInfo.getSerialNumber();
            }
        }
        return serialNumber;
    }

    @Override
    public @Nullable String getFirmwareVersion() {
        String firmware = null;
        if (this.cachedData != null) {
            TradfriCoapDeviceInfo deviceInfo = ((TradfriCoapDevice) this.cachedData).getDeviceInfo();
            if (deviceInfo != null) {
                firmware = deviceInfo.getFirmware();
            }
        }
        return firmware;
    }

    @Override
    public boolean isAlive() {
        boolean isAlive = false;
        if (this.cachedData != null) {
            isAlive = ((TradfriCoapDevice) this.cachedData).isAlive();
        }
        return isAlive;
    }

    @Override
    public int getBatteryLevel() {
        int batteryLevel = -1;
        if (this.cachedData != null) {
            TradfriCoapDeviceInfo deviceInfo = ((TradfriCoapDevice) this.cachedData).getDeviceInfo();
            if (deviceInfo != null) {
                batteryLevel = deviceInfo.getBatteryLevel();
            }
        }
        return batteryLevel;
    }

    @Override
    public TradfriCoapDevice parsePayload(String coapPayload) {
        return gson.fromJson(coapPayload, TradfriCoapDevice.class);
    }
}
