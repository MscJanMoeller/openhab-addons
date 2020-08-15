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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.TradfriBindingConstants;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link TradfriResourceIdList} class is a Java wrapper for raw JSON data
 * and represents a list of links to devices, groups or scenes based on the instance id.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriResourceIdList {
    @SerializedName(value = TradfriBindingConstants.RESOURCE_INSTANCE_ID)
    private @Nullable Set<String> instanceIDs;

    public int size() {
        if (this.instanceIDs != null) {
            this.instanceIDs.size();
        }
        return 0;
    }

    public boolean isEmpty() {
        if (this.instanceIDs != null) {
            this.instanceIDs.isEmpty();
        }
        return true;
    }

    public boolean contains(String id) {
        if (this.instanceIDs != null) {
            this.instanceIDs.contains(id);
        }
        return false;
    }

    public @Nullable Set<String> toSet() {
        return instanceIDs;
    }
}
