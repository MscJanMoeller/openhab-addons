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
package org.openhab.binding.tradfri.internal.coap.dto;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.TradfriBindingConstants;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link TradfriCoapDeviceInfo} class is used for a data transfer object (DTO) which contains data related to
 * device details.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapDeviceInfo {

    @SerializedName(value = TradfriBindingConstants.DEVICE_VENDOR)
    private @Nullable String vendor;
    @SerializedName(value = TradfriBindingConstants.DEVICE_MODEL)
    private @Nullable String model;
    @SerializedName(value = TradfriBindingConstants.DEVICE_SERIAL_NUMBER)
    private @Nullable String serialNumber;
    @SerializedName(value = TradfriBindingConstants.DEVICE_FIRMWARE)
    private @Nullable String firmware;
    @SerializedName(value = TradfriBindingConstants.DEVICE_POWER_SOURCE)
    private int powerSource = -1;
    @SerializedName(value = TradfriBindingConstants.DEVICE_BATTERY_LEVEL)
    private int batteryLevel = -1;

    public Optional<String> getVendor() {
        return Optional.ofNullable(this.vendor);
    }

    public Optional<String> getModel() {
        return Optional.ofNullable(this.model);
    }

    public Optional<String> getSerialNumber() {
        return Optional.ofNullable(this.serialNumber);
    }

    public Optional<String> getFirmware() {
        return Optional.ofNullable(this.firmware);
    }

    public int getPowerSource() {
        return powerSource;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }
}
