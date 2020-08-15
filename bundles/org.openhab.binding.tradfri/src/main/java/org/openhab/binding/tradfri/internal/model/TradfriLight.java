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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link TradfriLight} class is a base Java wrapper for raw JSON data related to a light bulb.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriLight extends TradfriDevice {

    // type OnOff struct {
    // On *YesNo `json:"5850,omitempty"`
    // }

    // type Dimmable struct {
    // OnOff
    // Dim *uint8 `json:"5851,omitempty"`
    // }

    // LightSetting
    // Color string `json:"5706,omitempty"`
    // Hue int `json:"5707,omitempty"`
    // Saturation int `json:"5708,omitempty"`
    // ColorX int `json:"5709,omitempty"`
    // ColorY int `json:"5710,omitempty"`

    // type Light struct {
    // LightSetting
    // TransitionTime *int `json:"5712,omitempty"`
    // CumulativeActivePower *float64 `json:"5805,omitempty"`
    // OnTime *int64 `json:"5852,omitempty"`
    // PowerFactor *float64 `json:"5820,omitempty"`
    // Unit *string `json:"5701,omitempty"`
    // }

}
