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

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
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
import org.openhab.binding.tradfri.internal.model.TradfriThingResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TradfriGroupHandler} is responsible for handling commands of individual groups.
 *
 * @author Jan Möller - Initial contribution
 */
@NonNullByDefault
public class TradfriGroupHandler extends TradfriThingResourceHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // step size for increase/decrease commands
    private static final PercentType STEP = new PercentType(10);

    public TradfriGroupHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected void onResourceUpdated(TradfriThingResource resource) {
        if (resource.matches(THING_TYPE_GROUP)) {
            resource.as(TradfriGroup.class).ifPresent(group -> onGroupUpdated(group));
        } else {
            // Delegate
            super.onResourceUpdated(resource);
        }
    }

    @TradfriEventHandler(EType.RESOURCE_UPDATED)
    public void onGroupUpdated(TradfriEvent event, TradfriGroup group) {
        onGroupUpdated(group);
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
            final Optional<TradfriGroup> group = getResourceAs(TradfriGroup.class);
            switch (channelUID.getId()) {
                case CHANNEL_BRIGHTNESS:
                    group.ifPresent(g -> handleBrightnessCommand(command, g));
                    break;
                case CHANNEL_COLOR_TEMPERATURE:
                    group.ifPresent(g -> handleColorTemperatureCommand(command, g));
                    break;
                case CHANNEL_COLOR:
                    group.ifPresent(g -> handleColorCommand(command, g));
                    break;
                case CHANNEL_SCENE:
                    group.ifPresent(g -> handleSceneCommand(command, g));
                    break;
                default:
                    logger.error("Unknown channel UID {}", channelUID);
            }
        } else {
            logger.debug("Gateway not online. Cannot handle command {} for channel {}", command, channelUID);
        }
    }

    private void onGroupUpdated(TradfriGroup group) {
        updateState(CHANNEL_BRIGHTNESS, group.getBrightness());
        logger.debug("Updated channel {} of group {} to {}}", CHANNEL_BRIGHTNESS, group.getInstanceId().get(),
                group.getBrightness());

        // TODO update channels: color_temperature, color

        group.getActiveScene().ifPresent(
                scene -> scene.getSceneName().ifPresent(name -> updateState(CHANNEL_SCENE, StringType.valueOf(name))));

        updateStatus(ThingStatus.ONLINE);
    }

    private void handleBrightnessCommand(Command command, TradfriGroup group) {
        if (command instanceof PercentType) {
            group.setBrightness((PercentType) command);
        } else if (command instanceof OnOffType) {
            group.setOnOff(((OnOffType) command));
        } else if (command instanceof IncreaseDecreaseType) {
            if (IncreaseDecreaseType.INCREASE.equals(command)) {
                group.increaseBrightnessBy(STEP);
            } else {
                group.decreaseBrightnessBy(STEP);
            }
        } else {
            logger.debug("Cannot handle command {} for channel {}", command, CHANNEL_BRIGHTNESS);
        }
    }

    private void handleColorTemperatureCommand(Command command, TradfriGroup group) {
        // TODO: implement command to set color temperature of group
        logger.info("Command {} for channel {} not implemented yet.", command, CHANNEL_COLOR_TEMPERATURE);
    }

    private void handleColorCommand(Command command, TradfriGroup group) {
        // TODO: implement command to set color of group
        logger.info("Command {} for channel {} not implemented yet.", command, CHANNEL_COLOR);
    }

    private void handleSceneCommand(Command command, TradfriGroup group) {
        // TODO: implement command to set scene of group
        if (command instanceof StringType) {
            logger.info("Command {} for channel {} not implemented yet.", command, CHANNEL_SCENE);
        } else {
            logger.error("Cannot handle command {} for channel {}", command, CHANNEL_SCENE);
        }
    }
}
