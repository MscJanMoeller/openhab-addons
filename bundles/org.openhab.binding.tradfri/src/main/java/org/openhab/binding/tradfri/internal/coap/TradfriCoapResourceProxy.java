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

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.coap.status.TradfriResource;
import org.openhab.binding.tradfri.internal.model.TradfriResourceEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriResourceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

/**
 * {@link TradfriCoapResourceProxy} observes changes of a single
 * resource like a device, group or scene.
 *
 * @author Jan Möller - Initial contribution
 *
 */

@NonNullByDefault
public abstract class TradfriCoapResourceProxy implements CoapHandler, TradfriResourceProxy {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final static Gson gson = new Gson();

    protected final ScheduledExecutorService scheduler;

    private final TradfriCoapClient coapClient;
    private @Nullable CoapObserveRelation observeRelation;

    private final Set<TradfriResourceEventHandler> updateHandler = new CopyOnWriteArraySet<>();

    protected @Nullable TradfriResource cachedData;

    protected TradfriCoapResourceProxy(TradfriCoapClient coapClient, ScheduledExecutorService scheduler) {
        this.coapClient = coapClient;
        this.scheduler = scheduler;
    }

    protected TradfriCoapResourceProxy(String uri, Endpoint endpoint, ScheduledExecutorService scheduler) {
        this.coapClient = new TradfriCoapClient(uri);
        this.coapClient.setEndpoint(endpoint);
        this.scheduler = scheduler;
    }

    @Override
    public @Nullable String getInstanceId() {
        String id = null;
        if (this.cachedData != null) {
            id = this.cachedData.getInstanceId();
        }
        return id;
    }

    @Override
    public @Nullable String getName() {
        String name = null;
        if (this.cachedData != null) {
            name = this.cachedData.getName();
        }
        return name;
    }

    public void observe() {
        scheduler.schedule(() -> {
            observeRelation = coapClient.observe(this);
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void triggerUpdate() {
        this.coapClient.get(this);
    }

    protected void updateData(TradfriResource data) {
        this.cachedData = data;
        updateHandler.forEach(listener -> listener.onUpdate(this));
    }

    @Override
    public void onLoad(@Nullable CoapResponse response) {
        if (response == null) {
            logger.trace("received empty CoAP response");
            return;
        }
        logger.debug("CoAP response\noptions: {}\npayload: {}", response.getOptions(), response.getResponseText());
        if (response.isSuccess()) {
            try {
                updateData(convert(response.getResponseText()));
            } catch (JsonParseException e) {
                logger.error("Coap response is no valid json: {}, {}", response.getResponseText(), e.getMessage());
            }
        } else {
            logger.debug("CoAP error {}", response.getCode());
            // TODO: implement generic error reaction for resource proxy
        }
    }

    @Override
    public void onError() {
        logger.warn("CoAP error. Failed to get resource update for {}.", this.coapClient.getURI());
        // TODO: implement generic error reaction for resource proxy
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
    public void registerHandler(TradfriResourceEventHandler handler) {
        this.updateHandler.add(handler);
    }

    /**
     * Unregisters a given handler.
     *
     * @param handler the handler to unregister
     */
    @Override
    public void unregisterHandler(TradfriResourceEventHandler handler) {
        this.updateHandler.remove(handler);
    }

    protected abstract TradfriResource convert(String coapPayload) throws JsonSyntaxException;
}
