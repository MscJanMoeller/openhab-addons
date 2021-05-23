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
 * The {@link TradfriCoapBlind} class is used for a data transfer object (DTO) which contains data related to a
 * blind or curtain that can be moved up and down. Also reports current battery level.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapBlind extends TradfriCoapDevice {

    @SerializedName(value = TradfriBindingConstants.BLINDS)
    private TradfriCoapBlindSetting @Nullable [] blindSettings;

    public Optional<TradfriCoapBlindSetting> getBlindSetting() {
        TradfriCoapBlindSetting[] blindSettingArray = this.blindSettings;
        if (blindSettingArray != null && blindSettingArray.length > 0) {
            return Optional.of(blindSettingArray[0]);
        } else {
            return Optional.empty();
        }
    }
}
