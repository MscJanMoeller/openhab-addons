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

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.TradfriBindingConstants;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link TradfriCoapResourceIdList} class is used for a data transfer object (DTO) which contains data
 * for a list of links to devices, groups or scenes based on the instance id.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapResourceIdList {
    private static final TradfriCoapResourceIdList EMPTY = new TradfriCoapResourceIdList();

    @SerializedName(value = TradfriBindingConstants.RESOURCE_INSTANCE_ID)
    private @Nullable Set<String> instanceIDs;

    public static TradfriCoapResourceIdList empty() {
        return EMPTY;
    }

    public int size() {
        int size = 0;
        if (this.instanceIDs != null) {
            size = this.instanceIDs.size();
        }
        return size;
    }

    public boolean isEmpty() {
        return size() > 0;
    }

    public boolean contains(String id) {
        boolean containsId = false;
        if (this.instanceIDs != null) {
            containsId = this.instanceIDs.contains(id);
        }
        return containsId;
    }

    public Set<String> toSet() {
        return Objects.requireNonNullElse(this.instanceIDs, Collections.emptySet());
    }
}
