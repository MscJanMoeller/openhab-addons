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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.TradfriBindingConstants;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link TradfriCoapLightSetting} class is used for a data transfer object (DTO) which contains data related to a
 * dimmable light bulb with color temperature settings and full color support.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapLightSetting {

    @SerializedName(value = TradfriBindingConstants.RESOURCE_INSTANCE_ID)
    public int instanceId;

    @SerializedName(value = TradfriBindingConstants.DIMMER)
    private int dimmer = -1;

    @SerializedName(value = TradfriBindingConstants.ONOFF)
    private int onOff = -1;

    @SerializedName(value = TradfriBindingConstants.COLOR_TEMPERATURE)
    private int colorTemperature = -1;

    @SerializedName(value = TradfriBindingConstants.COLOR_X)
    private int colorX = -1;
    @SerializedName(value = TradfriBindingConstants.COLOR_Y)
    private int colorY = -1;

    @SerializedName(value = TradfriBindingConstants.COLOR)
    private @Nullable String color;

    @SerializedName(value = TradfriBindingConstants.COLOR_HUE)
    private int hue = -1;
    @SerializedName(value = TradfriBindingConstants.COLOR_SATURATION)
    private int saturation = -1;

    public int getOnOff() {
        return this.onOff;
    }

    public int getDimmer() {
        return this.dimmer;
    }

    public int getColorTemperature() {
        return this.colorTemperature;
    }

    public int getColorX() {
        return this.colorX;
    }

    public int getColorY() {
        return this.colorY;
    }

    public @Nullable String getColor() {
        return this.color;
    }

    public int getHue() {
        return this.hue;
    }

    public int getSaturation() {
        return this.saturation;
    }

}
