/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import org.openhab.binding.tradfri.internal.coap.TradfriCoapClient;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapResourceCache;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapDevice;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapDeviceInfo;
import org.openhab.binding.tradfri.internal.model.TradfriDevice;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ThingTypeUID;

/**
 * {@link TradfriCoapDeviceProxy} observes changes of a single device
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public abstract class TradfriCoapDeviceProxy extends TradfriCoapThingResourceProxy implements TradfriDevice {

    protected TradfriCoapDeviceProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient,
            String coapPath, TradfriCoapDevice initialData, ThingTypeUID thingType) {
        super(resourceCache, coapClient, coapPath, initialData, thingType);
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
    public DecimalType getBatteryLevel() {
        return new DecimalType(getDeviceInfo().map(deviceInfo -> deviceInfo.getBatteryLevel()).orElse(-1));
    }

    @Override
    public OnOffType getBatteryLow() {
        return getDeviceInfo().map(deviceInfo -> deviceInfo.getBatteryLevel()).orElse(-1) < 10 ? OnOffType.ON
                : OnOffType.OFF;
    }

    @Override
    protected TradfriCoapDevice parsePayload(String coapPayload) {
        return dtoFrom(coapPayload, TradfriCoapDevice.class);
    }

    protected Optional<TradfriCoapDeviceInfo> getDeviceInfo() {
        return getDataAs(TradfriCoapDevice.class).flatMap(device -> device.getDeviceInfo());
    }
}
