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
import org.openhab.binding.tradfri.internal.model.TradfriResourceEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriResourceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TradfriResourceHandler} is the abstract base class for specific resource handlers.
 *
 * @author Jan Möller - Initial contribution
 */
@NonNullByDefault
public abstract class TradfriResourceHandler extends BaseThingHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private @Nullable TradfriResourceProxy proxy;

    public TradfriResourceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public synchronized void initialize() {

        String id = getResourceId();
        if (id == null) {
            updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.CONFIGURATION_ERROR,
                    String.format("Invalid config: configuration parameter 'id' is missing."));
            return;
        }

        Bridge tradfriGateway = getBridge();
        if (tradfriGateway == null) {
            updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.BRIDGE_UNINITIALIZED,
                    String.format("Unexpected initialization error: link to TRADFRI gateway is missing."));
            return;
        }

        TradfriGatewayHandler handler = (TradfriGatewayHandler) tradfriGateway.getHandler();
        if (handler != null) {
            handler.registerResourceUpdateHandler(id, getEventHandler());
        }
    }

    @Override
    public synchronized void dispose() {
        super.dispose();

        if (this.proxy != null) {
            this.proxy = null;
        }

        String id = getResourceId();
        Bridge tradfriGateway = getBridge();
        if (id != null && tradfriGateway != null) {
            TradfriGatewayHandler handler = (TradfriGatewayHandler) tradfriGateway.getHandler();
            if (handler != null) {
                handler.unregisterResourceUpdateHandler(id, getEventHandler());
            }
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

    protected abstract TradfriResourceEventHandler getEventHandler();

    protected abstract @Nullable String getResourceId();

    protected @Nullable TradfriResourceProxy getProxy() {
        return this.proxy;
    }

    protected void updateStatus(TradfriResourceProxy proxy) {
        this.proxy = proxy;

        ThingStatus status = getThing().getStatus();
        if (status != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
        }
    }
}
