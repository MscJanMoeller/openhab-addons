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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link TradfriEventHandler} will be informed about the creation or deletion
 * of resources like devices, groups or scenes.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
@FunctionalInterface
public interface TradfriEventHandler {
    /**
     * This method is called when a resource list was updated.
     *
     * @param event provides details of the event e.g. whether resource was added or removed
     *            and the related instance id of the resource
     */
    public void onEvent(TradfriEvent event);
}