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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapResource;
import org.openhab.binding.tradfri.internal.model.TradfriResource;
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
public abstract class TradfriCoapResourceProxy implements CoapHandler, TradfriResource {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final static Gson gson = new Gson();

    protected final ScheduledExecutorService scheduler;

    private final TradfriCoapClient coapClient;
    private @Nullable CoapObserveRelation observeRelation;

    protected final TradfriCoapResourceCache resourceCache;

    protected @Nullable TradfriCoapResource cachedData;

    protected TradfriCoapResourceProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient,
            ScheduledExecutorService scheduler) {
        this.resourceCache = resourceCache;
        this.coapClient = coapClient;
        this.scheduler = scheduler;
    }

    protected TradfriCoapResourceProxy(TradfriCoapResourceCache resourceCache, String uri, Endpoint endpoint,
            ScheduledExecutorService scheduler) {
        this.resourceCache = resourceCache;
        this.coapClient = new TradfriCoapClient(uri);
        this.coapClient.setEndpoint(endpoint);
        this.scheduler = scheduler;
    }

    public void initialize(TradfriCoapResource data) {
        this.cachedData = data;
        this.resourceCache.add(this);
        // Start observation of resource updates
        observe();
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

    @Override
    public void triggerUpdate() {
        // Asynchronous call
        this.coapClient.get(this);
    }

    public abstract TradfriCoapResource parsePayload(String coapPayload) throws JsonSyntaxException;

    @Override
    public void onLoad(@Nullable CoapResponse response) {
        if (response == null) {
            logger.trace("received empty CoAP response");
            return;
        }
        logger.debug("CoAP response\noptions: {}\npayload: {}", response.getOptions(), response.getResponseText());
        if (response.isSuccess()) {
            try {
                updateData(parsePayload(response.getResponseText()));
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

    @Override
    public void dispose() {
        if (this.observeRelation != null) {
            this.observeRelation.reactiveCancel();
            this.observeRelation = null;
        }
        this.coapClient.shutdown();
        this.cachedData = null;
    }

    protected void observe() {
        if (this.observeRelation != null) {
            this.observeRelation.reactiveCancel();
            this.observeRelation = null;
        }

        scheduler.schedule(() -> {
            observeRelation = coapClient.observe(this);
        }, 1, TimeUnit.SECONDS);
    }

    protected void updateData(TradfriCoapResource data) {
        this.cachedData = data;
        this.resourceCache.updated(this);
    }

}
