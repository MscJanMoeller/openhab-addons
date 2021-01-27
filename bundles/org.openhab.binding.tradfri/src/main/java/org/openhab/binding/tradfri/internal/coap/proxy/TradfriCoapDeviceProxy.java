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
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapClient;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapResourceCache;
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
public abstract class TradfriCoapDeviceProxy extends TradfriCoapThingResourceProxy implements TradfriDevice {

    protected TradfriCoapDeviceProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient,
            ScheduledExecutorService scheduler, TradfriCoapDevice initialData, ThingTypeUID thingType) {
        super(resourceCache, coapClient, scheduler, initialData, thingType);
    }

    @Override
    public Optional<String> getVendor() {
        return getDeviceInfo().flatMap(deviceInfo -> deviceInfo.getVendor());
    }

    @Override
    public Optional<String> getModel() {
        return getDeviceInfo().flatMap(deviceInfo -> deviceInfo.getModel());
    }

    @Override
    public Optional<String> getSerialNumber() {
        return getDeviceInfo().flatMap(deviceInfo -> deviceInfo.getSerialNumber());
    }

    @Override
    public Optional<String> getFirmwareVersion() {
        return getDeviceInfo().flatMap(deviceInfo -> deviceInfo.getFirmware());
    }

    @Override
    public boolean isAlive() {
        return getDataAs(TradfriCoapDevice.class).map(device -> device.isAlive()).orElse(false);
    }

    @Override
    public int getBatteryLevel() {
        return getDeviceInfo().map(deviceInfo -> deviceInfo.getBatteryLevel()).orElse(-1);
    }

    private Optional<TradfriCoapDeviceInfo> getDeviceInfo() {
        return getDataAs(TradfriCoapDevice.class).flatMap(device -> device.getDeviceInfo());
    }
}
