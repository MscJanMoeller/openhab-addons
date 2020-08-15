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
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.openhab.binding.tradfri.internal.handler.TradfriResourceEventHandler;
import org.openhab.binding.tradfri.internal.handler.TradfriResourceProxy;
import org.openhab.binding.tradfri.internal.model.TradfriResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

/**
 * {@link TradfriCoapResourceProxy} observes changes of a single
 * resource like a device, group or scene.
 *
 * @author Jan Möller - Initial contribution
 *
 */

@NonNullByDefault
public abstract class TradfriCoapResourceProxy<T extends TradfriResource>
        implements CoapCallback, TradfriResourceProxy<T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Gson gson = new Gson();

    protected final ScheduledExecutorService scheduler;

    private TradfriCoapClient coapClient;
    private @Nullable CoapObserveRelation observeRelation;

    private final Set<TradfriResourceEventHandler<T>> updateHandler = new CopyOnWriteArraySet<>();

    private @Nullable T cachedData;

    public TradfriCoapResourceProxy(String uri, Endpoint endpoint, ScheduledExecutorService scheduler) {
        this.coapClient = new TradfriCoapClient(uri);
        this.coapClient.setEndpoint(endpoint);
        this.scheduler = scheduler;
    }

    @Override
    public @Nullable T getData() {
        return cachedData;
    }

    public void observe() {
        scheduler.schedule(() -> {
            observeRelation = coapClient.startObserve(this);
        }, 1, TimeUnit.SECONDS);
    }

    public void triggerUpdate() {
        this.coapClient.asyncGet(this);
    }

    protected void updateData(T data) {
        this.cachedData = data;
        updateHandler.forEach(listener -> listener.onUpdate(data));
    }

    @Override
    public void onUpdate(JsonElement jsonData) {
        logger.debug("onUpdate response: {}", jsonData);

        try {
            T data = convert(jsonData);
            updateData(data);
        } catch (JsonSyntaxException ex) {
            logger.error("Unexpected data response: {}", jsonData);
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
        coapClient.shutdown();
    }

    /**
     * Registers a handler, which is informed about resource updates.
     *
     * @param handler the handler to register
     */
    @Override
    public void registerHandler(TradfriResourceEventHandler<T> handler) {
        this.updateHandler.add(handler);
    }

    /**
     * Unregisters a given handler.
     *
     * @param handler the handler to unregister
     */
    @Override
    public void unregisterHandler(TradfriResourceEventHandler<T> handler) {
        this.updateHandler.remove(handler);
    }

    protected abstract T convert(JsonElement data) throws JsonSyntaxException;
}
