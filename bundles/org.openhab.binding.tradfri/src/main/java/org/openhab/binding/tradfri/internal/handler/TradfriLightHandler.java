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
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.tradfri.internal.model.TradfriColorLight;
import org.openhab.binding.tradfri.internal.model.TradfriColorTempLight;
import org.openhab.binding.tradfri.internal.model.TradfriDimmableLight;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.openhab.binding.tradfri.internal.model.TradfriEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriThingResource;

/**
 * The {@link TradfriLightHandler} is responsible for handling commands for individual lights.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Holger Reichert - Support for color bulbs
 * @author Christoph Weitkamp - Restructuring and refactoring of the binding
 * @author Jan Möller - Refactoring of the binding to support groups and scenes
 */
@NonNullByDefault
public class TradfriLightHandler extends TradfriDeviceHandler {

    // step size for increase/decrease commands
    private static final PercentType STEP = new PercentType(10);

    public TradfriLightHandler(Thing thing) {
        super(thing);
    }

    @TradfriEventHandler(EType.RESOURCE_UPDATED)
    public void onLightUpdated(TradfriEvent event, TradfriDimmableLight bulb) {
        onDimmableLightUpdated(bulb);
    }

    @TradfriEventHandler(EType.RESOURCE_UPDATED)
    public void onLightUpdated(TradfriEvent event, TradfriColorTempLight bulb) {
        onColorTempLightUpdated(bulb);
    }

    @TradfriEventHandler(EType.RESOURCE_UPDATED)
    public void onLightUpdated(TradfriEvent event, TradfriColorLight bulb) {
        onColorLightUpdated(bulb);
    }

    @Override
    protected void onResourceUpdated(TradfriThingResource resource) {
        if (resource.matches(THING_TYPE_DIMMABLE_LIGHT)) {
            resource.as(TradfriDimmableLight.class).ifPresent(bulb -> onDimmableLightUpdated(bulb));
        } else if (resource.matches(THING_TYPE_COLOR_TEMP_LIGHT)) {
            resource.as(TradfriColorTempLight.class).ifPresent(bulb -> onColorTempLightUpdated(bulb));
        } else if (resource.matches(THING_TYPE_COLOR_LIGHT)) {
            resource.as(TradfriColorLight.class).ifPresent(bulb -> onColorLightUpdated(bulb));
        } else {
            // Delegate
            super.onResourceUpdated(resource);
        }
    }

    protected void onDimmableLightUpdated(TradfriDimmableLight bulb) {
        onDeviceUpdated(bulb);
        updateState(CHANNEL_BRIGHTNESS, bulb.getBrightness());
        logger.debug("Updated thing for light bulb with Id {} to state {dimmer: {}}", bulb.getInstanceId(),
                bulb.getBrightness());
    }

    protected void onColorTempLightUpdated(TradfriColorTempLight bulb) {
        onDeviceUpdated(bulb);
        updateState(CHANNEL_BRIGHTNESS, bulb.getBrightness());
        bulb.getColorTemperature().ifPresent(colorTemp -> updateState(CHANNEL_COLOR_TEMPERATURE, colorTemp));
        logger.debug("Updated thing for light bulb with Id {} to state {dimmer: {}, colorTemp: {}}",
                bulb.getInstanceId(), bulb.getBrightness(), bulb.getColorTemperature());
    }

    protected void onColorLightUpdated(TradfriColorLight bulb) {
        onDeviceUpdated(bulb);
        bulb.getColorTemperature().ifPresent(colorTemp -> updateState(CHANNEL_COLOR_TEMPERATURE, colorTemp));
        bulb.getColor().ifPresent(color -> updateState(CHANNEL_COLOR, color));
        logger.debug("Updated thing for light bulb with Id {} to state {colorTemp: {}, color: {}}",
                bulb.getInstanceId(), bulb.getColorTemperature(), bulb.getColor());
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
                    getResourceAs(TradfriDimmableLight.class).ifPresent(bulb -> handleBrightnessCommand(command, bulb));
                    break;
                case CHANNEL_COLOR_TEMPERATURE:
                    getResourceAs(TradfriColorTempLight.class)
                            .ifPresent(bulb -> handleColorTemperatureCommand(command, bulb));
                    break;
                case CHANNEL_COLOR:
                    getResourceAs(TradfriColorLight.class).ifPresent(bulb -> handleColorCommand(command, bulb));
                    break;
                default:
                    logger.error("Unknown channel UID {}", channelUID);
            }
        } else {
            logger.debug("Bridge not online. Cannot handle command {} for channel {}", command, channelUID);
        }
    }

    private void handleBrightnessCommand(Command command, TradfriDimmableLight bulb) {
        if (command instanceof PercentType) {
            bulb.setBrightness((PercentType) command);
        } else if (command instanceof OnOffType) {
            bulb.setOnOff(((OnOffType) command));
        } else if (command instanceof IncreaseDecreaseType) {
            if (IncreaseDecreaseType.INCREASE.equals(command)) {
                bulb.increaseBrightnessBy(STEP);
            } else {
                bulb.decreaseBrightnessBy(STEP);
            }
        } else {
            logger.debug("Cannot handle command {} for channel {}", command, CHANNEL_BRIGHTNESS);
        }
    }

    private void handleColorTemperatureCommand(Command command, TradfriColorTempLight bulb) {
        if (command instanceof PercentType) {
            bulb.setColorTemperature((PercentType) command);
        } else if (command instanceof IncreaseDecreaseType) {
            if (IncreaseDecreaseType.INCREASE.equals(command)) {
                bulb.increaseBrightnessBy(STEP);
            } else {
                bulb.decreaseBrightnessBy(STEP);
            }
        } else {
            logger.debug("Can't handle command {} on channel {}", command, CHANNEL_COLOR_TEMPERATURE);
        }
    }

    private void handleColorCommand(Command command, TradfriColorLight bulb) {
        if (command instanceof HSBType) {
            bulb.setColor((HSBType) command);
            // TODO: is this call required?
            bulb.setBrightness(((HSBType) command).getBrightness());
        } else if (command instanceof OnOffType) {
            bulb.setOnOff(((OnOffType) command));
        } else if (command instanceof PercentType) {
            // PaperUI sends PercentType on color channel when changing Brightness
            bulb.setBrightness((PercentType) command);
        } else if (command instanceof IncreaseDecreaseType) {
            if (IncreaseDecreaseType.INCREASE.equals(command)) {
                bulb.increaseBrightnessBy(STEP);
            } else {
                bulb.decreaseBrightnessBy(STEP);
            }
        } else {
            logger.debug("Can't handle command {} on channel {}", command, CHANNEL_COLOR);
        }
    }

    /**
     * Checks if this light supports color temperature.
     *
     * @return true if the light supports full color
     */
    private boolean hasThingColorTempSupport() {
        return thing.getThingTypeUID().getId().equals(THING_TYPE_COLOR_TEMP_LIGHT.getId());
    }

    private Optional<TradfriColorTempLight> getColorTempLight() {
        return getResourceAs(TradfriColorTempLight.class);
    }

    /**
     * Checks if this light supports full color.
     *
     * @return true if the light supports full color
     */
    private boolean hasThingColorSupport() {
        return thing.getThingTypeUID().getId().equals(THING_TYPE_COLOR_LIGHT.getId());
    }

    private Optional<TradfriColorLight> getColorLight() {
        return getResourceAs(TradfriColorLight.class);
    }
}
