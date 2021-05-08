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

import java.util.EnumSet;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.tradfri.internal.config.TradfriDeviceConfig;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.openhab.binding.tradfri.internal.model.TradfriEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriResource;
import org.openhab.binding.tradfri.internal.model.TradfriResourceCache;
import org.openhab.binding.tradfri.internal.model.TradfriThingResource;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TradfriThingResourceHandler} is the abstract base class for specific resource handlers.
 *
 * @author Jan Möller - Initial contribution
 */
@NonNullByDefault
public abstract class TradfriThingResourceHandler extends BaseThingHandler implements TradfriEventHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Optional<TradfriResourceCache> resourceCache;

    public TradfriThingResourceHandler(Thing thing) {
        super(thing);
        this.resourceCache = Optional.empty();
    }

    @Override
    public synchronized void initialize() {
        logger.trace("Start initializing thing with id {}", getThingId());

        final Bridge tradfriGateway = getBridge();
        if (tradfriGateway == null) {
            updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.BRIDGE_UNINITIALIZED,
                    String.format("Unexpected initialization error: link to TRADFRI gateway is missing."));
            return;
        }

        final TradfriGatewayHandler handler = (TradfriGatewayHandler) tradfriGateway.getHandler();
        if (handler != null) {
            this.resourceCache = Optional.of(handler.getResourceCache());
            handler.getResourceCache().subscribeEvents(getThingId(),
                    EnumSet.of(EType.RESOURCE_ADDED, EType.RESOURCE_UPDATED), this);
        }

        getResource().ifPresent(thingResource -> onResourceInitialized(thingResource));
    }

    @Override
    public synchronized void dispose() {
        getResourceCache().ifPresent((cache) -> cache.unsubscribeEvents(this));
        this.resourceCache = Optional.empty();

        super.dispose();
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

    @Override
    public void onEvent(TradfriEvent event) {
        if (getThingId().equals(event.getId())) {
            switch (event.getType()) {
                case RESOURCE_ADDED:
                    getResource().ifPresent(thingResource -> onResourceInitialized(thingResource));
                    break;
                case RESOURCE_UPDATED: {
                    getResource().ifPresent(thingResource -> onResourceUpdated(thingResource));
                    break;
                }
                default:
                    break;
            }
        }
    }

    protected ThingTypeUID getThingType() {
        return getThing().getThingTypeUID();
    }

    protected String getThingId() {
        return getConfig().get(TradfriDeviceConfig.CONFIG_ID).toString();
    }

    protected Optional<TradfriResourceCache> getResourceCache() {
        return this.resourceCache;
    }

    protected Optional<TradfriThingResource> getResource() {
        return getResourceAs(TradfriThingResource.class);
    }

    protected <T extends TradfriResource> Optional<T> getResourceAs(Class<T> resourceClass) {
        return getResourceCache().flatMap(cache -> cache.getAs(getThingId(), resourceClass));
    }

    protected void onResourceInitialized(TradfriThingResource thingResource) {
        logger.trace("Initializing resource: id={} type={}", thingResource.getInstanceId().get(), getThingType());
        if (thingResource.matches(getThingType())) {
            onResourceUpdated(thingResource);
        } else {
            getResourceCache().ifPresent(cache -> cache.unsubscribeEvents(this));
            updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.CONFIGURATION_ERROR, String.format(
                    "Configuration error. Thing type of thing with id {} doesn't match. Expected: {}  Actual: {}",
                    getThingId(), getThingType().getId(), thingResource.getThingType().getId()));
        }
    }

    protected void onResourceUpdated(TradfriThingResource resource) {
        updateStatus(ThingStatus.ONLINE);
    }
}
