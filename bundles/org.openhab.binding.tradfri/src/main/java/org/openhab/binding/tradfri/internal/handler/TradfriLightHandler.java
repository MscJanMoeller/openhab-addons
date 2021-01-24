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
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.openhab.binding.tradfri.internal.model.TradfriEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriLight;
import org.openhab.binding.tradfri.internal.model.TradfriThingResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // step size for increase/decrease commands
    private static final PercentType STEP = new PercentType(10);

    public TradfriLightHandler(Thing thing) {
        super(thing);
    }

    @TradfriEventHandler(EType.RESOURCE_UPDATED)
    public void onLightUpdated(TradfriEvent event, TradfriLight bulb) {
        onLightUpdated(bulb);
    }

    @Override
    protected void onResourceUpdated(TradfriThingResource resource) {
        if (resource.matchesOneOf(SUPPORTED_LIGHT_TYPES_UIDS)) {
            resource.as(TradfriLight.class).ifPresent(bulb -> onLightUpdated(bulb));
        } else {
            // Delegate
            super.onResourceUpdated(resource);
        }
    }

    protected void onLightUpdated(TradfriLight bulb) {
        onDeviceUpdated(bulb);

        updateState(CHANNEL_BRIGHTNESS, bulb.getBrightness());
        logger.debug("Updated channel {} of light bulb {} to {}}", CHANNEL_BRIGHTNESS, bulb.getInstanceId().get(),
                bulb.getBrightness());

        if (thingSupportsColorTemperature() && bulb.supportsColorTemperature()) {
            bulb.getColorTemperature().ifPresent(colorTemp -> updateState(CHANNEL_COLOR_TEMPERATURE, colorTemp));
            logger.debug("Updated channel {} of light bulb {} to {}}", CHANNEL_COLOR_TEMPERATURE,
                    bulb.getInstanceId().get(), bulb.getColorTemperature().get());
        }

        if (thingSupportsColor() & bulb.supportsColor()) {
            bulb.getColor().ifPresent(color -> updateState(CHANNEL_COLOR, color));
            logger.debug("Updated channel {} of light bulb {} to {}}", CHANNEL_COLOR, bulb.getInstanceId().get(),
                    bulb.getColor().get());
        }
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
            final Optional<TradfriLight> lightBulb = getResourceAs(TradfriLight.class);
            switch (channelUID.getId()) {
                case CHANNEL_BRIGHTNESS:
                    lightBulb.ifPresent(bulb -> handleBrightnessCommand(command, bulb));
                    break;
                case CHANNEL_COLOR_TEMPERATURE:
                    lightBulb.filter(bulb -> bulb.supportsColorTemperature())
                            .ifPresent(bulb -> handleColorTemperatureCommand(command, bulb));
                    break;
                case CHANNEL_COLOR:
                    lightBulb.filter(bulb -> bulb.supportsColor()).ifPresent(bulb -> handleColorCommand(command, bulb));
                    break;
                default:
                    logger.error("Unknown channel UID {}", channelUID);
            }
        } else

        {
            logger.debug("Bridge not online. Cannot handle command {} for channel {}", command, channelUID);
        }
    }

    private void handleBrightnessCommand(Command command, TradfriLight bulb) {
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

    private void handleColorTemperatureCommand(Command command, TradfriLight bulb) {
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

    private void handleColorCommand(Command command, TradfriLight bulb) {
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
     * @return true if the light supports color temperature
     */
    private boolean thingSupportsColorTemperature() {
        return thing.getThingTypeUID().getId().equals(THING_TYPE_COLOR_TEMP_LIGHT.getId());
    }

    /**
     * Checks if this light supports full color.
     *
     * @return true if the light supports full color
     */
    private boolean thingSupportsColor() {
        return thing.getThingTypeUID().getId().equals(THING_TYPE_COLOR_LIGHT.getId());
    }
}
