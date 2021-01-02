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
import org.openhab.binding.tradfri.internal.model.TradfriResource;
import org.openhab.binding.tradfri.internal.model.TradfriResourceCache;
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

    private @Nullable TradfriResourceCache resourceCache;

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
            this.resourceCache = handler.getResourceCache();
        }
    }

    @Override
    public synchronized void dispose() {
        super.dispose();

        this.resourceCache = null;
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

    protected abstract @Nullable String getResourceId();

    protected @Nullable TradfriResourceCache getresourceCache() {
        return this.resourceCache;
    }

    protected @Nullable TradfriResource getProxy() {
        TradfriResource proxy = null;
        String id = getResourceId();
        if (id != null && this.resourceCache != null) {
            proxy = this.resourceCache.get(id);
        }
        return proxy;
    }

    protected void updateOnlineStatus(TradfriResource proxy) {
        ThingStatus status = getThing().getStatus();
        if (status != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
        }
    }
}
