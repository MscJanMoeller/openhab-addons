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
 * The {@link TradfriColorLightSetting} class is a Java wrapper for raw JSON data related to a light bulb
 * with color temperature settings and full colors support.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriColorLightSetting extends TradfriDimmableLightSetting {

    @SerializedName(value = TradfriBindingConstants.COLOR)
    private @Nullable String color;

    @SerializedName(value = TradfriBindingConstants.COLOR_HUE)
    private int hue;
    @SerializedName(value = TradfriBindingConstants.COLOR_SATURATION)
    private int saturation;

    @SerializedName(value = TradfriBindingConstants.COLOR_X)
    private int colorX;
    @SerializedName(value = TradfriBindingConstants.COLOR_Y)
    private int colorY;

    public @Nullable String getColor() {
        return this.color;
    }

    public int getHue() {
        return this.hue;
    }

    public int getSaturation() {
        return this.saturation;
    }

    public int getColorX() {
        return this.colorX;
    }

    public int getColorY() {
        return this.colorY;
    }

}
