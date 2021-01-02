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
 * The {@link TradfriCoapColorTempLight} class is a Java wrapper for raw JSON data related to a light bulb
 * that supports different color temperature settings.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapColorTempLight extends TradfriCoapDevice {

    @SerializedName(value = TradfriBindingConstants.LIGHT)
    private TradfriCoapColorTempLightSetting @Nullable [] lightSettings;

    public @Nullable TradfriCoapColorTempLightSetting getLightSetting() {
        TradfriCoapColorTempLightSetting[] lightSettingArray = this.lightSettings;
        if (lightSettingArray != null && lightSettingArray.length > 0) {
            return lightSettingArray[0];
        } else {
            return null;
        }
    }
}
