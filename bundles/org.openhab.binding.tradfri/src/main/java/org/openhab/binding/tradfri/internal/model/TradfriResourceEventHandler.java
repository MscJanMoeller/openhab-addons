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

/**
 * {@link TradfriResourceEventHandler} can register at the {@link TradfriResourceProxy}
 * to be informed about updates of resources like devices, groups or scenes.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
@FunctionalInterface
public interface TradfriResourceEventHandler {

    /**
     * This method is called when resource data has been updated.
     *
     * @param proxy representing the updated the resource
     */

    void onUpdate(TradfriResourceProxy proxy);
}