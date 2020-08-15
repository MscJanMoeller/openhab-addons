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
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.tradfri.internal.model.TradfriGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TradfriGroupHandler} is responsible for handling commands of individual groups.
 *
 * @author Jan Möller - Initial contribution
 */
@NonNullByDefault
public class TradfriGroupHandler extends TradfriResourceHandler<TradfriGroup>
        implements TradfriResourceEventHandler<TradfriGroup> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public TradfriGroupHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected TradfriResourceEventHandler<TradfriGroup> getEventHandler() {
        return this;
    }

    @Override
    public void onUpdate(TradfriGroup groupData) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO Auto-generated method stub

    }
}
