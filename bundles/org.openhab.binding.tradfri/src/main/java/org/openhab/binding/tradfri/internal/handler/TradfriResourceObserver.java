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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.openhab.binding.tradfri.internal.CoapCallback;
import org.openhab.binding.tradfri.internal.TradfriCoapClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

/**
 * {@link TradfriResourceObserver} observes changes of a single
 * resource like a device, group or scene.
 *
 * @author Jan Möller - Initial contribution
 *
 */
public abstract class TradfriResourceObserver<T> implements CoapCallback {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Gson gson = new Gson();

    protected final ScheduledExecutorService scheduler;

    private TradfriCoapClient coapClient;
    private @Nullable CoapObserveRelation observeRelation;

    private final Set<TradfriResourceEventListener<T>> updateListeners = new CopyOnWriteArraySet<>();

    public TradfriResourceObserver(String uri, Endpoint endpoint, ScheduledExecutorService scheduler) {
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

        try {
            updateListeners.forEach(listener -> listener.onUpdate(convert(data)));
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
     * Registers a listener, which is informed about resource updates.
     *
     * @param listener the listener to register
     */
    public void registerListener(TradfriResourceEventListener<T> listener) {
        this.updateListeners.add(listener);
    }

    /**
     * Unregisters a given listener.
     *
     * @param listener the listener to unregister
     */
    public void unregisterListener(TradfriResourceEventListener<T> listener) {
        this.updateListeners.remove(listener);
    }

    protected abstract T convert(JsonElement data) throws JsonSyntaxException;
}
