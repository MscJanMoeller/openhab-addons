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

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.californium.core.network.Endpoint;
import org.openhab.binding.tradfri.internal.model.TradfriDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

/**
 * {@link TradfriDeviceObserver} observes changes of a single device
 *
 * @author Jan Möller - Initial contribution
 *
 */
public class TradfriDeviceObserver extends TradfriResourceObserver<TradfriDevice> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public TradfriDeviceObserver(String uri, Endpoint endpoint, ScheduledExecutorService scheduler) {
        super(uri, endpoint, scheduler);
    }

    @Override
    protected TradfriDevice convert(JsonElement data) {
        return gson.fromJson(data, TradfriDevice.class);
    }

}
