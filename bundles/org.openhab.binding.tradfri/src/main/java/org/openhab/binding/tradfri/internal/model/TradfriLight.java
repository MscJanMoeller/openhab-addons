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

package org.openhab.binding.tradfri.internal.model;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;

/**
 * {@link TradfriLight} represents a single light bulb that
 * - has continuous brightness control and/or
 * - supports different color temperature settings and/or
 * - support full color
 *
 * @author Jan Möller - Initial contribution
 *
 */

@NonNullByDefault
public interface TradfriLight extends TradfriDevice {

    boolean supportsColorTemperature();

    boolean supportsColor();

    boolean isOn();

    boolean isOff();

    void setOnOff(OnOffType value);

    PercentType getBrightness();

    void setBrightness(PercentType value);

    void increaseBrightnessBy(PercentType value);

    void decreaseBrightnessBy(PercentType value);

    Optional<PercentType> getColorTemperature();

    void setColorTemperature(PercentType value);

    void increaseColorTemperatureBy(PercentType value);

    void decreaseColorTemperatureBy(PercentType value);

    Optional<HSBType> getColor();

    void setColor(HSBType value);
}