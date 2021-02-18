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
import org.openhab.binding.tradfri.internal.TradfriBindingConstants;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link TradfriCoapScene} class is used for a data transfer object (DTO) which contains data related to a scene.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapScene extends TradfriCoapResource {

    @SerializedName(value = TradfriBindingConstants.SCENE_INDEX)
    private int sceneIndex;

    // TODO add light settings

    @SerializedName(value = TradfriBindingConstants.IKEA_MOODS)
    private int ikeaMood;

    public int getSceneIndex() {
        return sceneIndex;
    }

    public int getIkeaMood() {
        return ikeaMood;
    }
}
