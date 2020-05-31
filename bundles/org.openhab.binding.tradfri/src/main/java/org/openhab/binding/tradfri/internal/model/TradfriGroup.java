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
 * The {@link TradfriGroupTest} class is a base Java wrapper for raw JSON data related to a device.
 *
 * @author Jan Möller - Initial contribution
 */

public class TradfriGroup extends TradfriResource {

    private class DeviceLinks {
        @SerializedName(value = TradfriBindingConstants.RESOURCE_LINKS)
        private TradfriResourceIdList resourceLinks;
    }

    /**
     * onOff and brightness are currently not updated by the gateway. The group state
     * must be calculated based on the devices belonging to the group.
     */
    @SerializedName(value = TradfriBindingConstants.ONOFF)
    private int onOff;
    @SerializedName(value = TradfriBindingConstants.DIMMER)
    private int brightness;

    @SerializedName(value = TradfriBindingConstants.SCENE_ID)
    private String sceneId;
    @SerializedName(value = TradfriBindingConstants.GROUP_TYPE)
    private int groupType;
    @SerializedName(value = TradfriBindingConstants.GROUP_DEVICE_LINKS)
    private DeviceLinks deviceLinks;

    public String getSceneId() {
        return sceneId;
    }

    public int getGroupType() {
        return groupType;
    }

    public TradfriResourceIdList getMembers() {
        return deviceLinks.resourceLinks;
    }

}
