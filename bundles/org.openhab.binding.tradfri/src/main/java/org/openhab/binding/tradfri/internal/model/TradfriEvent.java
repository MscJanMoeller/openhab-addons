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
package org.openhab.binding.tradfri.internal.model;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * {@link TradfriEvent} represents an event for a specific Tradfri resource.
 *
 * @author Jan Möller - Initial contribution
 */
@NonNullByDefault
public class TradfriEvent {

    public enum EType {
        RESOURCE_ADDED,
        RESOURCE_UPDATED,
        RESOURCE_REMOVED
    }

    private final String id;
    private final EType type;

    private TradfriEvent(String id, EType event) {
        this.id = id;
        this.type = event;
    }

    public static TradfriEvent from(String id, EType type) {
        return new TradfriEvent(id, type);
    }

    public String getId() {
        return this.id;
    }

    public EType getType() {
        return this.type;
    }

    public boolean is(EType other) {
        return this.type == other;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TradfriEvent other = (TradfriEvent) obj;
        return Objects.equals(id, other.id) && type == other.type;
    }
}
