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
import org.openhab.binding.tradfri.internal.TradfriBindingConstants;
import org.openhab.binding.tradfri.internal.config.TradfriGroupConfig;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriGroup;
import org.openhab.binding.tradfri.internal.model.TradfriResource;
import org.openhab.binding.tradfri.internal.model.TradfriScene;

/**
 * The {@link TradfriGroupHandler} is responsible for handling commands of individual groups.
 *
 * @author Jan Möller - Initial contribution
 */
@NonNullByDefault
public class TradfriGroupHandler extends TradfriResourceHandler {

    // the unique instance id of the group
    protected @Nullable String id;

    public TradfriGroupHandler(Thing thing) {
        super(thing);
    }

    @Override
    public synchronized void initialize() {
        this.id = getConfigAs(TradfriGroupConfig.class).id;

        super.initialize();
    }

    @Override
    public synchronized void dispose() {
        super.dispose();

        this.id = null;
    }

    @Override
    protected @Nullable String getResourceId() {
        return this.id != null ? this.id : null;
    }

    @TradfriEventHandler(TradfriEvent.RESOURCE_UPDATED)
    public void onUpdate(TradfriGroup proxy) {
        updateOnlineStatus(proxy);

        TradfriScene activeScene = proxy.getActiveScene();
        if (activeScene != null) {
            String name = activeScene.getSceneName();
            if (name == null) {
                logger.debug("Unexpected error. Scene proxy with ID {} doesn't provide a name.",
                        activeScene.getInstanceId());
            }
            StringType scene = new StringType(name);
            updateState(TradfriBindingConstants.CHANNEL_SCENE, scene);

            logger.debug("Updating group \"{}\" with ID {}. Current scene: {}", proxy.getName(), proxy.getInstanceId(),
                    activeScene.getInstanceId());
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        Bridge gateway = getBridge();
        if (gateway != null && gateway.getStatus() == ThingStatus.ONLINE) {
            if (command instanceof RefreshType) {
                logger.debug("Refreshing channel {}", channelUID);
                TradfriResource proxy = getProxy();
                if (proxy != null) {
                    proxy.triggerUpdate();
                } else {
                    logger.debug("Unexpected error. Proxy object of group with ID {} not initialized.", this.id);
                }
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

    private @Nullable String getSceneName(String sceneId) {
        String name = null;
        TradfriGroup proxy = (TradfriGroup) getProxy();
        if (proxy != null) {
            TradfriScene sceneProxy = proxy.getSceneById(sceneId);
            if (sceneProxy != null) {
                name = sceneProxy.getSceneName();
                if (name == null) {
                    logger.debug("Unexpected error. Scene proxy with ID {} doesn't provide a name.", sceneId);
                }
            }
        } else {
            logger.debug("Unexpected error. Proxy object of group with ID {} not initialized.", this.id);
        }
        return name;
    }
}
