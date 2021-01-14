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
import org.eclipse.smarthome.core.library.types.PercentType;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapColorTempLight;

/**
 * {@link TradfriCoapColorTempLight} represents a light
 * that supports different color temperature settings.
 *
 * @author Jan Möller - Initial contribution
 *
 */

@NonNullByDefault
public interface TradfriColorTempLight extends TradfriDimmableLight {

    Optional<PercentType> getColorTemperature();

    void setColorTemperature(PercentType value);

    void increaseColorTemperatureBy(PercentType value);

    void decreaseColorTemperatureBy(PercentType value);
}