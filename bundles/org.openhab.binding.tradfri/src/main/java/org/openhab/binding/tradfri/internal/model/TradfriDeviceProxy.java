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
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * {@link TradfriDeviceProxy} represents of a single device
 *
 * @author Jan Möller - Initial contribution
 *
 */

@NonNullByDefault
public interface TradfriDeviceProxy extends TradfriResourceProxy {

    ThingTypeUID getThingType();

    @Nullable
    String getVendor();

    @Nullable
    String getModel();

    @Nullable
    String getSerialNumber();

    @Nullable
    String getFirmware();

    boolean isAlive();

    int getBatteryLevel();
}