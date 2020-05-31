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

import java.util.Set;

import org.openhab.binding.tradfri.internal.TradfriBindingConstants;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link TradfriResourceIdList} class is a Java wrapper for raw JSON data
 * and represents a list of links to devices, groups or scenes based on the instance id.
 *
 * @author Jan Möller - Initial contribution
 */

public class TradfriResourceIdList {
    @SerializedName(value = TradfriBindingConstants.RESOURCE_INSTANCE_ID)
    private Set<String> instanceIDs;

    public int size() {
        return instanceIDs.size();
    }

    public boolean isEmpty() {
        return instanceIDs.isEmpty();
    }

    public boolean contains(String id) {
        return instanceIDs.contains(id);
    }

    public Set<String> toSet() {
        return instanceIDs;
    }
}
