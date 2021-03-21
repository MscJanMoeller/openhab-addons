/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import static org.openhab.core.thing.Thing.*;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.tradfri.internal.model.TradfriDevice;
import org.openhab.binding.tradfri.internal.model.TradfriThingResource;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;

/**
 * The {@link TradfriDeviceHandler} is the abstract base class for individual device handlers.
 *
 * @author Jan Möller - Initial contribution
 */
@NonNullByDefault
public abstract class TradfriDeviceHandler extends TradfriThingResourceHandler {

    public TradfriDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected void onResourceUpdated(TradfriThingResource resource) {
        Optional<TradfriDevice> device = resource.as(TradfriDevice.class);
        if (device.isPresent()) {
            onDeviceUpdated(device.get());
        } else {
            super.onResourceUpdated(resource);
        }
    }

    protected void onDeviceUpdated(TradfriDevice device) {
        updateDeviceProperties(device);
        updateStatus(device.isAlive() ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
    }

    private void updateDeviceProperties(TradfriDevice proxy) {
        proxy.getVendor().ifPresent(vendor -> getThing().setProperty(PROPERTY_VENDOR, vendor));
        proxy.getModel().ifPresent(modelId -> getThing().setProperty(PROPERTY_MODEL_ID, modelId));
        proxy.getSerialNumber().ifPresent(serialNumber -> getThing().setProperty(PROPERTY_SERIAL_NUMBER, serialNumber));
        proxy.getFirmwareVersion()
                .ifPresent(firmwareVersion -> getThing().setProperty(PROPERTY_FIRMWARE_VERSION, firmwareVersion));
    }
}
