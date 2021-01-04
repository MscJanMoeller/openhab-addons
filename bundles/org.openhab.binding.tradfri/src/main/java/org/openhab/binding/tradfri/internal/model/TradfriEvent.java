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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * {@link TradfriEvent} This annotation type must be used to mark a
 * method as handler for Tradfri resource events. Only methods using this
 * annotation type will be called by the type publisher.
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

    private static final TradfriEvent EMPTY = new TradfriEvent();

    private final @Nullable String id;
    private final @Nullable EType type;

    private TradfriEvent() {
        this.id = null;
        this.type = null;
    }

    private TradfriEvent(String id) {
        this.id = id;
        this.type = null;
    }

    private TradfriEvent(EType event) {
        this.id = null;
        this.type = event;
    }

    private TradfriEvent(String id, EType event) {
        this.id = id;
        this.type = event;
    }

    public static TradfriEvent empty() {
        return EMPTY;
    }

    public static TradfriEvent from(String id) {
        return new TradfriEvent(id);
    }

    public static TradfriEvent from(EType type) {
        return new TradfriEvent(type);
    }

    public static TradfriEvent from(String id, EType type) {
        return new TradfriEvent(id, type);
    }

    public @Nullable String getId() {
        return this.id;
    }

    public @Nullable EType getType() {
        return this.type;
    }

    public boolean is(EType other) {
        return this.type == other;
    }

    public boolean covers(TradfriEvent other) {
        final String id = this.id;
        final EType type = this.type;

        boolean idMatch = id == null;
        boolean typeMatch = type == null;

        if (id != null) {
            final String otherId = other.getId();
            idMatch = (otherId != null) ? id.equals(otherId) : true;
        }
        if (type != null) {
            final EType otherType = other.getType();
            typeMatch = (otherType != null) ? (type == otherType) : true;
        }

        return idMatch && typeMatch;
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
        final String id = this.id;
        final TradfriEvent other = (TradfriEvent) obj;
        if (this.type != other.type) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final EType type = this.type;
        final String id = this.id;
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }
}
