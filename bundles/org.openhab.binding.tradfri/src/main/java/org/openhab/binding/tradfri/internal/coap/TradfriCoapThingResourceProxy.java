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

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.tradfri.internal.model.TradfriThingResource;

/**
 * {@link TradfriCoapThingResourceProxy} observes changes of a single device
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public abstract class TradfriCoapThingResourceProxy extends TradfriCoapResourceProxy implements TradfriThingResource {

    private final ThingTypeUID thingType;

    protected TradfriCoapThingResourceProxy(TradfriCoapResourceCache resourceCache, ThingTypeUID thingType,
            TradfriCoapClient coapClient, ScheduledExecutorService scheduler) {
        super(resourceCache, coapClient, scheduler);
        this.thingType = thingType;
    }

    @Override
    public ThingTypeUID getThingType() {
        return this.thingType;
    }

    @Override
    public boolean matches(ThingTypeUID thingType) {
        return this.thingType.equals(thingType);
    }
}
