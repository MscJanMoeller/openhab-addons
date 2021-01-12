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

import java.util.EnumSet;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;

/**
 * {@link TradfriEventSubscription} defines the events a subscriber is interested in.
 *
 * @author Jan Möller - Initial contribution
 */
@NonNullByDefault
public class TradfriEventSubscription {

    private static final TradfriEventSubscription ALL = new TradfriEventSubscription();

    private final @Nullable String id;
    private final EnumSet<EType> types;

    public TradfriEventSubscription() {
        this.id = null;
        this.types = EnumSet.noneOf(EType.class);
    }

    public TradfriEventSubscription(String id) {
        this.id = id;
        this.types = EnumSet.noneOf(EType.class);
    }

    public TradfriEventSubscription(EnumSet<EType> eventTypes) {
        this.id = null;
        this.types = eventTypes;
    }

    public TradfriEventSubscription(String id, EnumSet<EType> eventTypes) {
        this.id = id;
        this.types = eventTypes;
    }

    public static TradfriEventSubscription allEvents() {
        return ALL;
    }

    public boolean covers(EType other) {
        return this.types.contains(other);
    }

    public boolean covers(TradfriEvent event) {
        final String id = this.id;

        boolean idMatch = (id == null) || id.equals(event.getId());
        boolean typeMatch = this.types.isEmpty() || covers(event.getType());

        return idMatch && typeMatch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, types);
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
        TradfriEventSubscription other = (TradfriEventSubscription) obj;
        return Objects.equals(id, other.id) && Objects.equals(types, other.types);
    }
}
