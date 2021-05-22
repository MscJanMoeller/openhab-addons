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
import org.openhab.binding.tradfri.internal.model.TradfriDevice;
import org.openhab.binding.tradfri.internal.model.TradfriThingResource;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TradfriControllerHandler} is responsible for handling commands for individual controllers.
 *
 * @author Christoph Weitkamp - Initial contribution
 * @author Jan Möller - Refactored
 */
@NonNullByDefault
public class TradfriControllerHandler extends TradfriDeviceHandler {

    private final Logger logger = LoggerFactory.getLogger(TradfriControllerHandler.class);

    public TradfriControllerHandler(Thing thing) {
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
            logger.debug("The controller is a read-only device and cannot handle commands.");
        } else {
            logger.debug("Bridge not online. Cannot handle command {} for channel {}", command, channelUID);
        }
    }

    @Override
    protected void onResourceUpdated(TradfriThingResource resource) {
        if (resource.matchesOneOf(SUPPORTED_CONTROLLER_TYPES_UIDS)) {
            resource.as(TradfriDevice.class).ifPresent(controller -> onControllerUpdated(controller));
        } else {
            // Delegate
            super.onResourceUpdated(resource);
        }
    }

    protected void onControllerUpdated(TradfriDevice controller) {
        updateState(CHANNEL_BATTERY_LEVEL, controller.getBatteryLevel());
        logger.trace("Updated channel {} of controller {} to {}}", CHANNEL_BATTERY_LEVEL,
                controller.getInstanceId().get(), controller.getBatteryLevel());

        updateState(CHANNEL_BATTERY_LOW, controller.getBatteryLow());
        logger.trace("Updated channel {} of controller {} to {}}", CHANNEL_BATTERY_LOW,
                controller.getInstanceId().get(), controller.getBatteryLevel());

        onDeviceUpdated(controller);
    }
}
