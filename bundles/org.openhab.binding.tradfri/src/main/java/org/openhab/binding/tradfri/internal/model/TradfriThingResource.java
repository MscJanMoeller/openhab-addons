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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * {@link TradfriThingResource} extends {@link TradfriResource} to provide the matching {@link ThingTypeUID}.
 * An instance of {@link TradfriThingResource} represents a Tradfri resource mapped to a thing for openHAB.
 *
 * @author Jan Möller - Initial contribution
 *
 */

@NonNullByDefault
public interface TradfriThingResource extends TradfriResource {

    ThingTypeUID getThingType();

    boolean matches(ThingTypeUID thingType);

    boolean matchesOneOf(Set<ThingTypeUID> thingTypes);
}
