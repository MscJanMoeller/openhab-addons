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
 * The {@link TradfriCoapLight} class is used for a data transfer object (DTO) which contains data related to a
 * dimmable light bulb with color temperature settings and full color support.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapLight extends TradfriCoapDevice {

    @SerializedName(value = TradfriBindingConstants.LIGHT)
    private TradfriCoapLightSetting @Nullable [] lightSettings;

    public Optional<TradfriCoapLightSetting> getLightSetting() {
        TradfriCoapLightSetting[] lightSettingArray = this.lightSettings;
        if (lightSettingArray != null && lightSettingArray.length > 0) {
            return Optional.of(lightSettingArray[0]);
        } else {
            return Optional.empty();
        }
    }
}
