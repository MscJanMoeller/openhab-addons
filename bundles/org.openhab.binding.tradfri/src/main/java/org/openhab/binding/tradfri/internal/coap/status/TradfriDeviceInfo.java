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
package org.openhab.binding.tradfri.internal.coap.status;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.TradfriBindingConstants;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link TradfriDeviceInfo} class is a base Java wrapper for raw JSON data related to specific devices info.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriDeviceInfo {

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

    public @Nullable String getVendor() {
        return vendor;
    }

    public @Nullable String getModel() {
        return model;
    }

    public @Nullable String getSerialNumber() {
        return serialNumber;
    }

    public @Nullable String getFirmware() {
        return firmware;
    }

    public int getPowerSource() {
        return powerSource;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }
}
