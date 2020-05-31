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
 * The {@link TradfriResource} class is a base Java wrapper for raw JSON data related to devices, groups and scenes.
 *
 * @author Jan Möller - Initial contribution
 */

public class TradfriResource {

    @SerializedName(value = TradfriBindingConstants.RESOURCE_NAME)
    public String name;
    @SerializedName(value = TradfriBindingConstants.RESOURCE_TIMESTAMP_CREATED_AT)
    private long timestampCreatedAt;
    @SerializedName(value = TradfriBindingConstants.RESOURCE_INSTANCE_ID)
    private String instanceID;

    public String getName() {
        return name;
    }

    public long getTimestampCreatedAt() {
        return timestampCreatedAt;
    }

    public String getInstanceID() {
        return instanceID;
    }

}
