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

/**
 * {@link TradfriEvent} This annotation type must be used to mark a
 * method as handler for Tradfri resource events. Only methods using this
 * annotation type will be called by the event publisher.
 *
 * @author Jan Möller - Initial contribution
 */

public enum TradfriEvent {
    RESOURCE_ADDED,
    RESOURCE_UPDATED,
    RESOURCE_REMOVED
}