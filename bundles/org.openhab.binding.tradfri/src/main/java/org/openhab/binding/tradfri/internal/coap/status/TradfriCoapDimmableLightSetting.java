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
import org.openhab.binding.tradfri.internal.TradfriBindingConstants;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link TradfriCoapDimmableLightSetting} class is a base Java wrapper for raw JSON data related to a dimmable light
 * bulb.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapDimmableLightSetting {

    @SerializedName(value = TradfriBindingConstants.RESOURCE_INSTANCE_ID)
    public int instanceId;

    @SerializedName(value = TradfriBindingConstants.DIMMER)
    private int dimmer;

    @SerializedName(value = TradfriBindingConstants.ONOFF)
    private int onOff;

    public int getOnOff() {
        return this.onOff;
    }

    public int getDimmer() {
        return this.dimmer;
    }
}
