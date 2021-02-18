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

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.TradfriBindingConstants;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link TradfriCoapResource} class is used for a data transfer object (DTO) which contains data related to
 * devices, groups and scenes.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapResource {

    @SerializedName(value = TradfriBindingConstants.RESOURCE_NAME)
    private @Nullable String name;
    @SerializedName(value = TradfriBindingConstants.RESOURCE_TIMESTAMP_CREATED_AT)
    private long timestampCreatedAt;
    @SerializedName(value = TradfriBindingConstants.RESOURCE_INSTANCE_ID)
    private @Nullable String instanceId;

    public <T extends TradfriCoapResource> Optional<T> as(Class<T> resourceClass) {
        return resourceClass.isAssignableFrom(getClass()) ? Optional.of(resourceClass.cast(this)) : Optional.empty();
    }

    public Optional<String> getName() {
        return (this.name != null) ? Optional.of(this.name) : Optional.empty();
    }

    public long getTimestampCreatedAt() {
        return timestampCreatedAt;
    }

    public Optional<String> getInstanceId() {
        return (this.instanceId != null) ? Optional.of(this.instanceId) : Optional.empty();
    }
}
