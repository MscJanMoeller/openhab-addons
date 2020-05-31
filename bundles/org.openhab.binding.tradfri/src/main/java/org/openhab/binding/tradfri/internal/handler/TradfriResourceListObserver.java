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

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.openhab.binding.tradfri.internal.CoapCallback;
import org.openhab.binding.tradfri.internal.TradfriCoapClient;
import org.openhab.binding.tradfri.internal.handler.TradfriResourceListEventListener.ResourceListEvent;
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

    private final Set<TradfriResourceListEventListener> updateListeners = new CopyOnWriteArraySet<>();

    private Set<String> cachedResources = Collections.emptySet();

    public TradfriResourceListObserver(String uri, Endpoint endpoint, ScheduledExecutorService scheduler) {
        this.coapClient = new TradfriCoapClient(uri);
        this.coapClient.setEndpoint(endpoint);
        this.scheduler = scheduler;
    }

    public void observe() {
        scheduler.schedule(() -> {
            observeRelation = coapClient.startObserve(this);
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onUpdate(JsonElement data) {
        logger.debug("onUpdate response: {}", data);

        if (data.isJsonArray()) {
            Type setType = new TypeToken<Set<String>>() {
            }.getType();
            Set<String> currentResources = gson.fromJson(data, setType);

            Set<String> addedResources = currentResources.stream().filter(s -> !cachedResources.contains(s))
                    .collect(Collectors.toSet());
            Set<String> removedResources = cachedResources.stream().filter(s -> !currentResources.contains(s))
                    .collect(Collectors.toSet());

            addedResources.forEach(
                    id -> updateListeners.forEach(listener -> listener.onUpdate(ResourceListEvent.RESOURCE_ADDED, id)));

            removedResources.forEach(id -> updateListeners
                    .forEach(listener -> listener.onUpdate(ResourceListEvent.RESOURCE_REMOVED, id)));

            this.cachedResources = currentResources;
        }
    }

    @Override
    public void onError(ThingStatus status, ThingStatusDetail statusDetail) {
        logger.warn("CoAP error. Failed to get resource list update for {}.", this.coapClient.getURI());
    }

    public void dispose() {
        if (observeRelation != null) {
            observeRelation.reactiveCancel();
            observeRelation = null;
        }
        if (coapClient != null) {
            coapClient.shutdown();
        }
    }

    /**
     * Registers a listener, which is informed about resource list changes.
     *
     * @param listener the listener to register
     */
    public void registerListener(TradfriResourceListEventListener listener) {
        this.updateListeners.add(listener);
    }

    /**
     * Unregisters a given listener.
     *
     * @param listener the listener to unregister
     */
    public void unregisterListener(TradfriResourceListEventListener listener) {
        this.updateListeners.remove(listener);
    }
}
