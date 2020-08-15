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
package org.openhab.binding.tradfri.internal.coap;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link TradfriResourceListEventHandler} can register at the {@link TradfriResourceListObserver}
 * to be informed about the creation or deletion of resources like devices, groups or scenes.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
@FunctionalInterface
public interface TradfriResourceListEventHandler {

    enum ResourceListEvent {
        RESOURCE_ADDED,
        RESOURCE_REMOVED
    }

    /**
     * This method is called when new device information is received.
     *
     * @param event defines whether a resource was added or removed
     * @param instanceId The instance id of the device
     */
    public void onUpdate(ResourceListEvent event, String instanceId);
}