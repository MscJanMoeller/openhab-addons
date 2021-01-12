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

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.openhab.binding.tradfri.internal.model.TradfriEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriGroup;

/**
 * The {@link TradfriGroupHandler} is responsible for handling commands of individual groups.
 *
 * @author Jan Möller - Initial contribution
 */
@NonNullByDefault
public class TradfriGroupHandler extends TradfriThingResourceHandler {

    // the unique instance id of the group
    protected @Nullable String id;

    public TradfriGroupHandler(Thing thing) {
        super(thing);
    }

    @TradfriEventHandler(EType.RESOURCE_UPDATED)
    public void onGroupUpdated(TradfriEvent event, TradfriGroup group) {
        onResourceUpdated(group);

        group.getActiveScene().ifPresent(
                scene -> scene.getSceneName().ifPresent(name -> updateState(CHANNEL_SCENE, StringType.valueOf(name))));

        // TODO update channels: brightness, color_temperature, color
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
                case CHANNEL_BRIGHTNESS:
                    handleBrightnessCommand(command);
                    break;
                case CHANNEL_COLOR_TEMPERATURE:
                    handleColorTemperatureCommand(command);
                    break;
                case CHANNEL_COLOR:
                    handleColorCommand(command);
                    break;
                case CHANNEL_SCENE:
                    handleSceneCommand(command);
                    break;
                default:
                    logger.error("Unknown channel UID {}", channelUID);
            }
        } else {
            logger.debug("Gateway not online. Cannot handle command {} for channel {}", command, channelUID);
        }
    }

    private void handleBrightnessCommand(Command command) {
        // TODO: implement command to set brightness of a group
        if (command instanceof PercentType) {
            logger.info("Command {} for channel {} not implemented yet.", command, CHANNEL_BRIGHTNESS);
        } else if (command instanceof OnOffType) {
            logger.info("Command {} for channel {} not implemented yet.", command, CHANNEL_BRIGHTNESS);
        } else if (command instanceof IncreaseDecreaseType) {
            logger.info("Command {} for channel {} not implemented yet.", command, CHANNEL_BRIGHTNESS);
        } else {
            logger.error("Cannot handle command {} for channel {}", command, CHANNEL_BRIGHTNESS);
        }
    }

    private void handleColorTemperatureCommand(Command command) {
        // TODO: implement command to set color temperature of group
        logger.info("Command {} for channel {} not implemented yet.", command, CHANNEL_COLOR_TEMPERATURE);
    }

    private void handleColorCommand(Command command) {
        // TODO: implement command to set color of group
        logger.info("Command {} for channel {} not implemented yet.", command, CHANNEL_COLOR);
    }

    private void handleSceneCommand(Command command) {
        // TODO: implement command to set scene of group
        if (command instanceof StringType) {
            logger.info("Command {} for channel {} not implemented yet.", command, CHANNEL_SCENE);
        } else {
            logger.error("Cannot handle command {} for channel {}", command, CHANNEL_SCENE);
        }
    }
}
