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

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;

/**
 * {@link TradfriResourceCache} stores all objects of specific
 * single resources like a device, group or scene.
 *
 * @author Jan Möller - Initial contribution
 *
 */

@NonNullByDefault
public interface TradfriResourceCache {

    void subscribeEvents(Object subscriber);

    void subscribeEvents(String id, Object subscriber);

    void subscribeEvents(EnumSet<EType> eventTypes, Object subscriber);

    void subscribeEvents(String id, EType eventType, Object subscriber);

    void subscribeEvents(String id, EnumSet<EType> eventTypes, Object subscriber);

    void unsubscribeEvents(Object subscriber);

    void unsubscribeEvents(String id, EType eventType, Object subscriber);

    boolean contains(String id);

    Optional<? extends TradfriResource> get(String id);

    <T extends TradfriResource> Optional<T> getAs(String id, Class<T> resourceClass);

    <T extends TradfriResource> Stream<T> streamOf(Class<T> resourceClass);

    <T extends TradfriResource> Stream<T> streamOf(Class<T> resourceClass, Predicate<T> predicate);

    void refresh();

    void clear();
}
