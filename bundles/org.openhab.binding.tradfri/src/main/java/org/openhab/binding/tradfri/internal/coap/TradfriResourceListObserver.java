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

package org.openhab.binding.tradfri.internal.coap;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

/**
 * {@link TradfriResourceListObserver} observes a list of resources of the gateway
 * and generates events for the creation and deletion of devices, groups or scenes.
 *
 * @author Jan Möller - Initial contribution
 *
 */
public class TradfriResourceListObserver implements CoapCallback {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Gson gson = new Gson();

    protected final ScheduledExecutorService scheduler;

    private TradfriCoapClient coapClient;
    private @Nullable ScheduledFuture<?> updateJob;

    private final Set<TradfriResourceListEventHandler> updateHandler = new CopyOnWriteArraySet<>();

    // Stores resource IDs
    private Set<String> cachedResources = Collections.emptySet();

    private int updateCounter = 0;

    private int POLL_PERIOD = 60;

    public TradfriResourceListObserver(String uri, Endpoint endpoint, ScheduledExecutorService scheduler) {
        this.coapClient = new TradfriCoapClient(uri);
        this.coapClient.setEndpoint(endpoint);
        this.scheduler = scheduler;
    }

    /**
     * Checks if this observer is initialized
     *
     * @return <code>true</code> if at least one valid response was received from the gateway;
     *         <code>false</code> otherwise.
     */
    public boolean isInitialized() {
        return this.updateCounter > 0;
    }

    public synchronized void observe() {
        /**
         * The native CoAP observe mechanism is currently not supported by the TRADFRI gateway
         * for lists of devices, groups and scenes. Therefore the ResourceListObserver are polling
         * the gateway every POLL_PERIOD seconds for changes.
         */
        if (this.updateJob == null) {
            this.updateJob = this.scheduler.scheduleWithFixedDelay(this::triggerUpdate, 1, POLL_PERIOD,
                    TimeUnit.SECONDS);
        }
    }

    public void triggerUpdate() {
        this.coapClient.asyncGet(this);
    }

    @Override
    public synchronized void onUpdate(JsonElement data) {
        logger.debug("onUpdate response: {}", data);

        if (data.isJsonArray()) {
            Type setType = new TypeToken<Set<String>>() {
            }.getType();
            Set<String> currentResources = gson.fromJson(data, setType);

            // Inform listener about added resources
            currentResources.stream().filter(id -> !cachedResources.contains(id)).forEach(id -> updateHandler
                    .forEach(listener -> listener.onUpdate(TradfriEvent.from(id, EType.RESOURCE_ADDED))));

            // Inform listener about removed resources
            cachedResources.stream().filter(id -> !currentResources.contains(id)).forEach(id -> updateHandler
                    .forEach(listener -> listener.onUpdate(TradfriEvent.from(id, EType.RESOURCE_REMOVED))));

            this.cachedResources = currentResources;
        }
        updateCounter++;
    }

    @Override
    public synchronized void onError(ThingStatus status, ThingStatusDetail statusDetail) {
        logger.warn("CoAP error. Failed to get resource list update for {}.", this.coapClient.getURI());
    }

    public synchronized void dispose() {
        updateCounter = 0;

        this.cachedResources.forEach(id -> updateHandler
                .forEach(listener -> listener.onUpdate(TradfriEvent.from(id, EType.RESOURCE_REMOVED))));

        this.cachedResources.clear();
        this.updateHandler.clear();

        if (this.updateJob != null) {
            this.updateJob.cancel(true);
            this.updateJob = null;
        }

        if (this.coapClient != null) {
            this.coapClient.shutdown();
        }
    }

    /**
     * Registers a handler, which will be informed about resource list changes.
     *
     * @param handler the handler to register
     */
    public void registerHandler(TradfriResourceListEventHandler handler) {
        this.updateHandler.add(handler);
    }

    /**
     * Unregisters a given handler.
     *
     * @param handler the handler to unregister
     */
    public void unregisterHandler(TradfriResourceListEventHandler handler) {
        this.updateHandler.remove(handler);
    }
}
