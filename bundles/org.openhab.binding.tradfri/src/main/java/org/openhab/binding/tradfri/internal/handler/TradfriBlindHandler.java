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
package org.openhab.binding.tradfri.internal.handler;

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.tradfri.internal.model.TradfriBlind;
import org.openhab.binding.tradfri.internal.model.TradfriThingResource;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TradfriBlindHandler} is responsible for handling commands for individual blinds.
 *
 * @author Manuel Raffel - Initial contribution
 * @author Jan Möller - Refactored
 */
@NonNullByDefault
public class TradfriBlindHandler extends TradfriDeviceHandler {

    private final Logger logger = LoggerFactory.getLogger(TradfriBlindHandler.class);

    public TradfriBlindHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        Bridge gateway = getBridge();
        if (gateway != null && gateway.getStatus() == ThingStatus.ONLINE) {
            if (command instanceof RefreshType) {
                logger.debug("Refreshing channel {}", channelUID);
                getResource().ifPresent(resource -> resource.triggerUpdate());
                return;
            }
            switch (channelUID.getId()) {
                case CHANNEL_POSITION:
                    getResourceAs(TradfriBlind.class).ifPresent(blind -> handlePositionCommand(command, blind));
                    break;
                default:
                    logger.error("Unknown channel UID {}", channelUID);
            }
        } else {
            logger.debug("Bridge not online. Cannot handle command {} for channel {}", command, channelUID);
        }
    }

    @Override
    protected void onResourceUpdated(TradfriThingResource resource) {
        if (resource.matches(THING_TYPE_BLINDS)) {
            resource.as(TradfriBlind.class).ifPresentOrElse(blind -> onBlindUpdated(blind),
                    () -> super.onResourceUpdated(resource));
        } else {
            // Delegate
            super.onResourceUpdated(resource);
        }
    }

    protected void onBlindUpdated(TradfriBlind blind) {
        updateState(CHANNEL_POSITION, blind.getPosition());
        logger.trace("Updated channel {} of blind {} to {}}", CHANNEL_POSITION, blind.getInstanceId().orElse("-1"),
                blind.getBatteryLevel());

        updateState(CHANNEL_BATTERY_LEVEL, blind.getBatteryLevel());
        logger.trace("Updated channel {} of blind {} to {}}", CHANNEL_BATTERY_LEVEL, blind.getInstanceId().orElse("-1"),
                blind.getBatteryLevel());

        updateState(CHANNEL_BATTERY_LOW, blind.getBatteryLow());
        logger.trace("Updated channel {} of blind {} to {}}", CHANNEL_BATTERY_LOW, blind.getInstanceId().orElse("-1"),
                blind.getBatteryLevel());

        onDeviceUpdated(blind);
    }

    private void handlePositionCommand(Command command, TradfriBlind blind) {
        if (command instanceof PercentType) {
            blind.setPosition((PercentType) command);
        } else if (command instanceof StopMoveType) {
            if (StopMoveType.STOP.equals(command)) {
                blind.stop();
            } else {
                logger.debug("Cannot handle command '{}' for channel '{}'", command, CHANNEL_POSITION);
            }
        } else if (command instanceof UpDownType) {
            if (UpDownType.UP.equals(command)) {
                blind.setPosition(PercentType.ZERO);
            } else {
                blind.setPosition(PercentType.HUNDRED);
            }
        } else {
            logger.debug("Cannot handle command '{}' for channel '{}'", command, CHANNEL_POSITION);
        }
    }
}
