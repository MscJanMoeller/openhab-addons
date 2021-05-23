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
 * The {@link TradfriControllerHandler} is responsible for handling commands for individual sensor.
 *
 * @author Christoph Weitkamp - Initial contribution
 * @author Jan Möller - Refactored
 */
@NonNullByDefault
public class TradfriSensorHandler extends TradfriDeviceHandler {

    private final Logger logger = LoggerFactory.getLogger(TradfriSensorHandler.class);

    public TradfriSensorHandler(Thing thing) {
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
            logger.debug("The sensor is a read-only device and cannot handle commands.");
        } else {
            logger.debug("Bridge not online. Cannot handle command {} for channel {}", command, channelUID);
        }
    }

    @Override
    protected void onResourceUpdated(TradfriThingResource resource) {
        if (resource.matchesOneOf(SUPPORTED_SENSOR_TYPES_UIDS)) {
            resource.as(TradfriDevice.class).ifPresentOrElse(sensor -> onSensorUpdated(sensor),
                    () -> super.onResourceUpdated(resource));
        } else {
            // Delegate
            super.onResourceUpdated(resource);
        }
    }

    protected void onSensorUpdated(TradfriDevice sensor) {
        updateState(CHANNEL_BATTERY_LEVEL, sensor.getBatteryLevel());
        logger.trace("Updated channel {} of sensor {} to {}}", CHANNEL_BATTERY_LEVEL,
                sensor.getInstanceId().orElse("-1"), sensor.getBatteryLevel());

        updateState(CHANNEL_BATTERY_LOW, sensor.getBatteryLow());
        logger.trace("Updated channel {} of sensor {} to {}}", CHANNEL_BATTERY_LOW, sensor.getInstanceId().orElse("-1"),
                sensor.getBatteryLevel());

        onDeviceUpdated(sensor);
    }
}
