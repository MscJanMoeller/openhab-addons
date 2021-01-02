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
 * The {@link TradfriCoapColorTempLightSetting} class is a Java wrapper for raw JSON data related to a light bulb
 * that supports different color temperature settings.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapColorTempLightSetting extends TradfriCoapDimmableLightSetting {

    @SerializedName(value = TradfriBindingConstants.COLOR)
    private @Nullable String color;

    @SerializedName(value = TradfriBindingConstants.COLOR_X)
    private int colorX;
    @SerializedName(value = TradfriBindingConstants.COLOR_Y)
    private int colorY;

    @SerializedName(value = TradfriBindingConstants.COLOR_TEMPERATURE)
    private int colorTemperature;

    public @Nullable String getColor() {
        return this.color;
    }

    public int getColorX() {
        return this.colorX;
    }

    public int getColorY() {
        return this.colorY;
    }

    public int getColorTemperature() {
        return this.colorTemperature;
    }

}
