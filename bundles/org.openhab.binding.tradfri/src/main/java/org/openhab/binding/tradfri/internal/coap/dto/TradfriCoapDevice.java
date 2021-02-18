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
package org.openhab.binding.tradfri.internal.coap.dto;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.TradfriBindingConstants;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link TradfriCoapDevice} class is used for a data transfer object (DTO) which contains data related to a device.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapDevice extends TradfriCoapResource {

    public enum DeviceType {
        SWITCH,
        REMOTE,
        LIGHT,
        PLUG,
        SENSOR,
        REPEATER,
        BLIND,
        UNKNOWN
    }

    @SerializedName(value = TradfriBindingConstants.DEVICE_TYPE)
    private int deviceType;

    @SerializedName(value = TradfriBindingConstants.REACHABILITY_STATE)
    private int reachabilityState;

    @SerializedName(value = TradfriBindingConstants.TIMESTAMP_LAST_SEEN)
    private long timestampLastSeen;

    @SerializedName(value = TradfriBindingConstants.OTA_UPDATE_STATE)
    private int currentOtaUpdateState;

    @SerializedName(value = TradfriBindingConstants.DEVICE)
    private @Nullable TradfriCoapDeviceInfo deviceInfo;

    public DeviceType getDeviceType() {
        switch (deviceType) {
            case TradfriBindingConstants.DEVICE_TYPE_SWITCH:
                return DeviceType.SWITCH;
            case TradfriBindingConstants.DEVICE_TYPE_REMOTE:
                return DeviceType.REMOTE;
            case TradfriBindingConstants.DEVICE_TYPE_LIGHT:
                return DeviceType.LIGHT;
            case TradfriBindingConstants.DEVICE_TYPE_PLUG:
                return DeviceType.PLUG;
            case TradfriBindingConstants.DEVICE_TYPE_SENSOR:
                return DeviceType.SENSOR;
            case TradfriBindingConstants.DEVICE_TYPE_REPEATER:
                return DeviceType.REPEATER;
            case TradfriBindingConstants.DEVICE_TYPE_BLINDS:
                return DeviceType.BLIND;
        }
        return DeviceType.UNKNOWN;
    }

    public int getReachabilityState() {
        return reachabilityState;
    }

    public long getTimestampLastSeen() {
        return timestampLastSeen;
    }

    public int getCurrentOtaUpdateState() {
        return currentOtaUpdateState;
    }

    public Optional<TradfriCoapDeviceInfo> getDeviceInfo() {
        return (this.deviceInfo != null) ? Optional.of(this.deviceInfo) : Optional.empty();
    }

    public boolean isAlive() {
        return getReachabilityState() == TradfriBindingConstants.REACHABILITY_STATE_ALIVE;
    }

}
