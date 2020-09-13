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

import static org.eclipse.smarthome.core.thing.Thing.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.tradfri.internal.config.TradfriDeviceConfig;
import org.openhab.binding.tradfri.internal.model.TradfriDevice;
import org.openhab.binding.tradfri.internal.model.TradfriDeviceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TradfriDeviceHandler} is the abstract base class for individual device handlers.
 *
 * @author Jan Möller - Initial contribution
 */
@NonNullByDefault
public abstract class TradfriDeviceHandler<T extends TradfriDevice> extends TradfriResourceHandler<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // the unique instance id of the device
    protected @Nullable Integer id;

    public TradfriDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public synchronized void initialize() {
        this.id = getConfigAs(TradfriDeviceConfig.class).id;

        super.initialize();
    }

    @Override
    public synchronized void dispose() {
        super.dispose();

        this.id = null;
    }

    @Override
    protected @Nullable String getResourceId() {
        return this.id != null ? this.id.toString() : null;
    }

    protected void set(String payload) {
        logger.debug("Sending payload: {}", payload);
        // TODO: coapClient.asyncPut(payload, this, scheduler);
    }

    protected void updateDeviceProperties(TradfriDeviceData state) {
        String firmwareVersion = state.getFirmwareVersion();
        if (firmwareVersion != null) {
            getThing().setProperty(PROPERTY_FIRMWARE_VERSION, firmwareVersion);
        }

        String modelId = state.getModelId();
        if (modelId != null) {
            getThing().setProperty(PROPERTY_MODEL_ID, modelId);
        }

        String vendor = state.getVendor();
        if (vendor != null) {
            getThing().setProperty(PROPERTY_VENDOR, vendor);
        }
    }
}
