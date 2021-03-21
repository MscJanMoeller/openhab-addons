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

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link TradfriResource} is a base class of a single resource
 * like a device, group or scene.
 *
 * @author Jan Möller - Initial contribution
 *
 */

@NonNullByDefault
public interface TradfriResource {

    <T extends TradfriResource> Optional<T> as(Class<T> resourceClass);

    Optional<String> getInstanceId();

    Optional<String> getName();

    void triggerUpdate();

    void dispose();
}
