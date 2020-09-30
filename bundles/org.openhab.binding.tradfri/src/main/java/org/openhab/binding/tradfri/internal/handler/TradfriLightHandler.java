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
import org.openhab.binding.tradfri.internal.model.TradfriLightData;
import org.openhab.binding.tradfri.internal.model.TradfriResourceEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriResourceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

/**
 * The {@link TradfriLightHandler} is responsible for handling commands for individual lights.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Holger Reichert - Support for color bulbs
 * @author Christoph Weitkamp - Restructuring and refactoring of the binding
 */
@NonNullByDefault
public class TradfriLightHandler extends TradfriDeviceHandler implements TradfriResourceEventHandler {

    private final Logger logger = LoggerFactory.getLogger(TradfriLightHandler.class);

    // step size for increase/decrease commands
    private static final int STEP = 10;

    // keeps track of the current state for handling of increase/decrease
    private @Nullable TradfriLightData state;

    public TradfriLightHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected TradfriResourceEventHandler getEventHandler() {
        return this;
    }

    @Override
    public void onUpdate(TradfriResourceProxy proxy) {
        // TODO Auto-generated method stub
    }

    public void onUpdate(JsonElement data) {
        if (!(data.isJsonNull())) {
            TradfriLightData state = new TradfriLightData(data);
            updateStatus(state.getReachabilityStatus() ? ThingStatus.ONLINE : ThingStatus.OFFLINE);

            if (!state.getOnOffState()) {
                logger.debug("Setting state to OFF");
                updateState(CHANNEL_BRIGHTNESS, PercentType.ZERO);
                if (lightHasColorSupport()) {
                    updateState(CHANNEL_COLOR, HSBType.BLACK);
                }
                // if we are turned off, we do not set any brightness value
                return;
            }

            PercentType dimmer = state.getBrightness();
            if (dimmer != null && !lightHasColorSupport()) { // color lights do not have brightness channel
                updateState(CHANNEL_BRIGHTNESS, dimmer);
            }

            PercentType colorTemp = state.getColorTemperature();
            if (colorTemp != null) {
                updateState(CHANNEL_COLOR_TEMPERATURE, colorTemp);
            }

            HSBType color = null;
            if (lightHasColorSupport()) {
                color = state.getColor();
                if (color != null) {
                    updateState(CHANNEL_COLOR, color);
                }
            }

            updateDeviceProperties(state);

            this.state = state;

            logger.debug(
                    "Updating thing for lightId {} to state {dimmer: {}, colorTemp: {}, color: {}, firmwareVersion: {}, modelId: {}, vendor: {}}",
                    state.getDeviceId(), dimmer, colorTemp, color, state.getFirmwareVersion(), state.getModelId(),
                    state.getVendor());
        }
    }

    private void setBrightness(PercentType percent) {
        TradfriLightData data = new TradfriLightData();
        data.setBrightness(percent).setTransitionTime(DEFAULT_DIMMER_TRANSITION_TIME);
        set(data.getJsonString());
    }

    private void setState(OnOffType onOff) {
        TradfriLightData data = new TradfriLightData();
        data.setOnOffState(onOff == OnOffType.ON);
        set(data.getJsonString());
    }

    private void setColorTemperature(PercentType percent) {
        TradfriLightData data = new TradfriLightData();
        data.setColorTemperature(percent).setTransitionTime(DEFAULT_DIMMER_TRANSITION_TIME);
        set(data.getJsonString());
    }

    private void setColor(HSBType hsb) {
        TradfriLightData data = new TradfriLightData();
        data.setColor(hsb).setTransitionTime(DEFAULT_DIMMER_TRANSITION_TIME);
        set(data.getJsonString());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        Bridge gateway = getBridge();
        if (gateway != null && gateway.getStatus() == ThingStatus.ONLINE) {
            if (command instanceof RefreshType) {
                logger.debug("Refreshing channel {}", channelUID);
                // TODO: coapClient.asyncGet(this);
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
                default:
                    logger.error("Unknown channel UID {}", channelUID);
            }
        } else {
            logger.debug("Bridge not online. Cannot handle command {} for channel {}", command, channelUID);
        }
    }

    private void handleBrightnessCommand(Command command) {
        if (command instanceof PercentType) {
            setBrightness((PercentType) command);
        } else if (command instanceof OnOffType) {
            setState(((OnOffType) command));
        } else if (command instanceof IncreaseDecreaseType) {
            final TradfriLightData state = this.state;
            if (state != null && state.getBrightness() != null) {
                @SuppressWarnings("null")
                int current = state.getBrightness().intValue();
                if (IncreaseDecreaseType.INCREASE.equals(command)) {
                    setBrightness(new PercentType(Math.min(current + STEP, PercentType.HUNDRED.intValue())));
                } else {
                    setBrightness(new PercentType(Math.max(current - STEP, PercentType.ZERO.intValue())));
                }
            } else {
                logger.debug("Cannot handle inc/dec as current state is not known.");
            }
        } else {
            logger.debug("Cannot handle command {} for channel {}", command, CHANNEL_BRIGHTNESS);
        }
    }

    private void handleColorTemperatureCommand(Command command) {
        if (command instanceof PercentType) {
            setColorTemperature((PercentType) command);
        } else if (command instanceof IncreaseDecreaseType) {
            final TradfriLightData state = this.state;
            if (state != null && state.getColorTemperature() != null) {
                @SuppressWarnings("null")
                int current = state.getColorTemperature().intValue();
                if (IncreaseDecreaseType.INCREASE.equals(command)) {
                    setColorTemperature(new PercentType(Math.min(current + STEP, PercentType.HUNDRED.intValue())));
                } else {
                    setColorTemperature(new PercentType(Math.max(current - STEP, PercentType.ZERO.intValue())));
                }
            } else {
                logger.debug("Cannot handle inc/dec as current state is not known.");
            }
        } else {
            logger.debug("Can't handle command {} on channel {}", command, CHANNEL_COLOR_TEMPERATURE);
        }
    }

    private void handleColorCommand(Command command) {
        if (command instanceof HSBType) {
            setColor((HSBType) command);
            setBrightness(((HSBType) command).getBrightness());
        } else if (command instanceof OnOffType) {
            setState(((OnOffType) command));
        } else if (command instanceof PercentType) {
            // PaperUI sends PercentType on color channel when changing Brightness
            setBrightness((PercentType) command);
        } else if (command instanceof IncreaseDecreaseType) {
            final TradfriLightData state = this.state;
            // increase or decrease only the brightness, but keep color
            if (state != null && state.getBrightness() != null) {
                @SuppressWarnings("null")
                int current = state.getBrightness().intValue();
                if (IncreaseDecreaseType.INCREASE.equals(command)) {
                    setBrightness(new PercentType(Math.min(current + STEP, PercentType.HUNDRED.intValue())));
                } else {
                    setBrightness(new PercentType(Math.max(current - STEP, PercentType.ZERO.intValue())));
                }
            } else {
                logger.debug("Cannot handle inc/dec for color as current brightness is not known.");
            }
        } else {
            logger.debug("Can't handle command {} on channel {}", command, CHANNEL_COLOR);
        }
    }

    /**
     * Checks if this light supports full color.
     *
     * @return true if the light supports full color
     */
    private boolean lightHasColorSupport() {
        return thing.getThingTypeUID().getId().equals(THING_TYPE_COLOR_LIGHT.getId());
    }
}
