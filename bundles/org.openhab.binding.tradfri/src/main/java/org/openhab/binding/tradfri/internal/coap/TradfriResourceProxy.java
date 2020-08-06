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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.openhab.binding.tradfri.internal.handler.TradfriResourceEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

/**
 * {@link TradfriResourceProxy} observes changes of a single
 * resource like a device, group or scene.
 *
 * @author Jan Möller - Initial contribution
 *
 */
public abstract class TradfriResourceProxy<T> implements CoapCallback {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Gson gson = new Gson();

    protected final ScheduledExecutorService scheduler;

    private TradfriCoapClient coapClient;
    private @Nullable CoapObserveRelation observeRelation;

    private final Set<TradfriResourceEventHandler<T>> updateHandler = new CopyOnWriteArraySet<>();

    private @Nullable T cachedData;

    public TradfriResourceProxy(String uri, Endpoint endpoint, ScheduledExecutorService scheduler) {
        this.coapClient = new TradfriCoapClient(uri);
        this.coapClient.setEndpoint(endpoint);
        this.scheduler = scheduler;
    }

    public void observe() {
        scheduler.schedule(() -> {
            observeRelation = coapClient.startObserve(this);
        }, 1, TimeUnit.SECONDS);
    }

    public T getData() {
        return cachedData;
    }

    protected void updateData(T data) {
        this.cachedData = data;
    }

    @Override
    public void onUpdate(JsonElement data) {
        logger.debug("onUpdate response: {}", data);

        try {
            updateData(convert(data));
            updateHandler.forEach(listener -> listener.onUpdate(getData()));
        } catch (JsonSyntaxException ex) {
            logger.error("Unexpected data response: {}", data);
        }
    }

    @Override
    public void onError(ThingStatus status, ThingStatusDetail statusDetail) {
        logger.warn("CoAP error. Failed to get resource update for {}.", this.coapClient.getURI());
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
     * Registers a handler, which is informed about resource updates.
     *
     * @param handler the handler to register
     */
    public void registerHandler(TradfriResourceEventHandler<T> handler) {
        this.updateHandler.add(handler);
    }

    /**
     * Unregisters a given handler.
     *
     * @param handler the handler to unregister
     */
    public void unregisterHandler(TradfriResourceEventHandler<T> handler) {
        this.updateHandler.remove(handler);
    }

    protected abstract T convert(JsonElement data) throws JsonSyntaxException;
}
