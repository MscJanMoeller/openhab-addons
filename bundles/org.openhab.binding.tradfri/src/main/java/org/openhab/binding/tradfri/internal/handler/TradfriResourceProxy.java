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

package org.openhab.binding.tradfri.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.model.TradfriResource;

/**
 * {@link TradfriResourceProxy} represents of a single resource
 * like a device, group or scene and provides:
 * - access to the data
 * - notifies about changes
 * - forwards commands
 *
 * @author Jan Möller - Initial contribution
 *
 */

@NonNullByDefault
public interface TradfriResourceProxy<T extends TradfriResource> {

    @Nullable
    T getData();

    /**
     * Registers a handler, which is informed about resource updates.
     *
     * @param handler the handler to register
     */
    void registerHandler(TradfriResourceEventHandler<T> handler);

    /**
     * Unregisters a given handler.
     *
     * @param handler the handler to unregister
     */
    void unregisterHandler(TradfriResourceEventHandler<T> handler);

}