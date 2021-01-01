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
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.openhab.binding.tradfri.internal.config.TradfriDeviceConfig;
import org.openhab.binding.tradfri.internal.model.TradfriDeviceData;
import org.openhab.binding.tradfri.internal.model.TradfriDeviceProxy;
import org.openhab.binding.tradfri.internal.model.TradfriResourceProxy;

/**
 * The {@link TradfriDeviceHandler} is the abstract base class for individual device handlers.
 *
 * @author Jan Möller - Initial contribution
 */
@NonNullByDefault
public abstract class TradfriDeviceHandler extends TradfriResourceHandler {

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

    protected void onUpdate(TradfriDeviceProxy device) {
        updateOnlineStatus(device);
        updateStatus(device.isAlive() ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
        updateDeviceProperties(device);
    }

    @Override
    protected @Nullable String getResourceId() {
        return this.id != null ? this.id.toString() : null;
    }

    protected boolean updateDeviceStatus(TradfriResourceProxy proxy) {

        boolean success = false;

        if (proxy instanceof TradfriDeviceProxy) {
            TradfriDeviceProxy device = (TradfriDeviceProxy) proxy;

            // Check match of thing type
            if (getThing().getThingTypeUID().equals(device.getThingType())) {

                success = true;

            } else {
                logger.error("Thing type mismatch during device update. Expected:'{}' Actual:'{}'",
                        getThing().getThingTypeUID().getId(), device.getThingType().getId());
            }
        } else {
            logger.error("Got device update from unknown thing type. Expected: '{}'",
                    getThing().getThingTypeUID().getId());
        }

        return success;
    }

    private void updateDeviceProperties(TradfriDeviceProxy proxy) {
        String vendor = proxy.getVendor();
        if (vendor != null) {
            getThing().setProperty(PROPERTY_VENDOR, vendor);
        }

        String modelId = proxy.getModel();
        if (modelId != null) {
            getThing().setProperty(PROPERTY_MODEL_ID, modelId);
        }

        String serialNumber = proxy.getSerialNumber();
        if (serialNumber != null) {
            getThing().setProperty(PROPERTY_SERIAL_NUMBER, serialNumber);
        }

        String firmwareVersion = proxy.getFirmwareVersion();
        if (firmwareVersion != null) {
            getThing().setProperty(PROPERTY_FIRMWARE_VERSION, firmwareVersion);
        }
    }

    // TODO: remove
    protected void set(String payload) {
        logger.debug("Sending payload: {}", payload);
        // TODO: coapClient.asyncPut(payload, this, scheduler);
    }

    // TODO: remove
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
