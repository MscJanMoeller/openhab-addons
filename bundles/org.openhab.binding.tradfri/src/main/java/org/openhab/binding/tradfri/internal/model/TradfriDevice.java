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
package org.openhab.binding.tradfri.internal.model;

import org.openhab.binding.tradfri.internal.TradfriBindingConstants;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link TradfriDevice} class is a base Java wrapper for raw JSON data related to a device.
 *
 * @author Jan Möller - Initial contribution
 */

public class TradfriDevice extends TradfriResource {

    // type OnOff struct {
    // On *YesNo `json:"5850,omitempty"`
    // }

    // type Dimmable struct {
    // OnOff
    // Dim *uint8 `json:"5851,omitempty"`
    // }

    // LightSetting
    // Color string `json:"5706,omitempty"`
    // Hue int `json:"5707,omitempty"`
    // Saturation int `json:"5708,omitempty"`
    // ColorX int `json:"5709,omitempty"`
    // ColorY int `json:"5710,omitempty"`

    // type Light struct {
    // LightSetting
    // TransitionTime *int `json:"5712,omitempty"`
    // CumulativeActivePower *float64 `json:"5805,omitempty"`
    // OnTime *int64 `json:"5852,omitempty"`
    // PowerFactor *float64 `json:"5820,omitempty"`
    // Unit *string `json:"5701,omitempty"`
    // }

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
    public int deviceType;

    @SerializedName(value = TradfriBindingConstants.REACHABILITY_STATE)
    public int reachabilityState;

    @SerializedName(value = TradfriBindingConstants.TIMESTAMP_LAST_SEEN)
    public long timestampLastSeen;

    @SerializedName(value = TradfriBindingConstants.OTA_UPDATE_STATE)
    public int currentOtaUpdateState;

    @SerializedName(value = TradfriBindingConstants.DEVICE)
    public TradfriDeviceInfo deviceInfo;

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

    public TradfriDeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public boolean isAlive() {
        return getReachabilityState() == TradfriBindingConstants.REACHABILITY_STATE_ALIVE;
    }

}
