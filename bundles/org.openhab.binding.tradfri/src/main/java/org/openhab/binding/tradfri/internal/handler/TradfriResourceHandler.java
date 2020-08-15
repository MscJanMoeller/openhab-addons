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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.openhab.binding.tradfri.internal.config.TradfriDeviceConfig;
import org.openhab.binding.tradfri.internal.model.TradfriResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TradfriResourceHandler} is the abstract base class for individual resource handlers.
 *
 * @author Jan Möller - Initial contribution
 */
@NonNullByDefault
public abstract class TradfriResourceHandler<T extends TradfriResource> extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // the unique instance id of the device
    protected @Nullable Integer id;

    private @Nullable TradfriResourceProxy<T> proxy;

    public TradfriResourceHandler(Thing thing) {
        super(thing);
    }

    @Override
    @SuppressWarnings("null")
    public synchronized void initialize() {
        this.id = getConfigAs(TradfriDeviceConfig.class).id;

        updateStatus(ThingStatus.UNKNOWN);

        Bridge tradfriGateway = getBridge();
        switch (tradfriGateway.getStatus()) {
            case ONLINE:
                TradfriGatewayHandler handler = (TradfriGatewayHandler) tradfriGateway.getHandler();
                this.proxy = handler.getTradfriResource(this.id.toString());
                if (this.proxy != null) {
                    this.proxy.registerHandler(getEventHandler());
                } else {
                    // TODO: error handling
                }
                break;
            case OFFLINE:
            default:
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                        String.format("Gateway offline '%s'", tradfriGateway.getStatusInfo()));
                break;
        }
    }

    @Override
    public synchronized void dispose() {
        super.dispose();

        this.id = null;

        if (this.proxy != null) {
            this.proxy.unregisterHandler(getEventHandler());
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        // the status might have changed because the bridge is completely reconfigured
        if (bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
            dispose();
        } else if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            initialize();
        }
    }

    protected @Nullable TradfriResourceProxy<? extends TradfriResource> getProxy() {
        return this.proxy;
    }

    protected abstract TradfriResourceEventHandler<T> getEventHandler();
}
