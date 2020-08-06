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
import java.util.stream.Collectors;

import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.openhab.binding.tradfri.internal.coap.TradfriResourceListEventHandler.ResourceListEvent;
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

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Gson gson = new Gson();

    protected final ScheduledExecutorService scheduler;

    private TradfriCoapClient coapClient;
    private @Nullable CoapObserveRelation observeRelation;
    private @Nullable ScheduledFuture<?> updateJob;

    private final Set<TradfriResourceListEventHandler> updateHandler = new CopyOnWriteArraySet<>();

    private Set<String> cachedResources = Collections.emptySet();

    private int POLL_PERIOD = 60;

    public TradfriResourceListObserver(String uri, Endpoint endpoint, ScheduledExecutorService scheduler) {
        this.coapClient = new TradfriCoapClient(uri);
        this.coapClient.setEndpoint(endpoint);
        this.scheduler = scheduler;
    }

    public void observe() {
        /**
         * The native CoAP observe mechanism is currently not supported by the TRADFRI gateway
         * for lists of devices, groups and scenes. Therefore the ResourceListObserver are polling
         * the gateway every POLL_PERIOD seconds for changes.
         */
        if (this.updateJob != null) {
            this.updateJob = this.scheduler.scheduleWithFixedDelay(this::triggerUpdate, 1, POLL_PERIOD,
                    TimeUnit.SECONDS);
        }

        /**
         * The following code can be enabled if the TRADFRI gateway will support the
         * native CoAP observe mechanism for lists of devices, groups and scenes.
         *
         * this.scheduler.schedule(() -> {
         * this.observeRelation = this.coapClient.startObserve(this);
         * }, 1, TimeUnit.SECONDS);
         *
         */
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

            Set<String> addedResources = currentResources.stream().filter(r -> !cachedResources.contains(r))
                    .collect(Collectors.toSet());
            Set<String> removedResources = cachedResources.stream().filter(r -> !currentResources.contains(r))
                    .collect(Collectors.toSet());

            addedResources.forEach(
                    id -> updateHandler.forEach(listener -> listener.onUpdate(ResourceListEvent.RESOURCE_ADDED, id)));

            removedResources.forEach(id -> updateHandler
                    .forEach(listener -> listener.onUpdate(ResourceListEvent.RESOURCE_REMOVED, id)));

            this.cachedResources = currentResources;
        }
    }

    @Override
    public void onError(ThingStatus status, ThingStatusDetail statusDetail) {
        logger.warn("CoAP error. Failed to get resource list update for {}.", this.coapClient.getURI());
    }

    public void dispose() {
        if (this.updateJob != null) {
            this.updateJob.cancel(true);
            this.updateJob = null;
        }

        if (this.observeRelation != null) {
            this.observeRelation.reactiveCancel();
            this.observeRelation = null;
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
