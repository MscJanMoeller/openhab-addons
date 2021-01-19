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

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
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

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final static Gson gson = new Gson();

    protected final ScheduledExecutorService scheduler;

    private final TradfriCoapClient coapClient;
    private @Nullable CoapObserveRelation observeRelation;

    private final TradfriCoapResourceCache resourceCache;

    private TradfriCoapResource cachedData;

    protected TradfriCoapResourceProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient,
            ScheduledExecutorService scheduler, TradfriCoapResource initialData) {
        this.resourceCache = resourceCache;
        this.coapClient = coapClient;
        this.scheduler = scheduler;
        this.cachedData = initialData;
    }

    @Override
    public <T extends TradfriResource> Optional<T> as(Class<T> resourceClass) {
        return resourceClass.isAssignableFrom(getClass()) ? Optional.of(resourceClass.cast(this)) : Optional.empty();
    }

    public void initialize() {
        // Start observation of resource updates
        observe();
    }

    @Override
    public Optional<String> getInstanceId() {
        return this.cachedData.getInstanceId();
    }

    @Override
    public Optional<String> getName() {
        return this.cachedData.getName();
    }

    @Override
    public void triggerUpdate() {
        // Asynchronous call
        this.coapClient.get(this);
    }

    @Override
    public void onLoad(@Nullable CoapResponse response) {
        if (response == null) {
            logger.trace("received empty CoAP response");
            return;
        }
        logger.trace("Processing CoAP response. Options: {}  Payload: {}", response.getOptions(),
                response.getResponseText());
        if (response.isSuccess()) {
            try {
                updateData(parsePayload(response.getResponseText()));
            } catch (JsonParseException e) {
                logger.error("Coap response is no valid json: {}, {}", response.getResponseText(), e.getMessage());
            }
        } else {
            logger.debug("CoAP error: '{}' '{}'  Options: {}  Payload: {}", response.getCode(),
                    response.getCode().name(), response.getOptions(), response.getResponseText());
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
    }

    protected TradfriCoapResourceCache getResourceCache() {
        return this.resourceCache;
    }

    protected <T extends TradfriCoapResource> Optional<T> getDataAs(Class<T> resourceClass) {
        return this.cachedData.as(resourceClass);
    }

    protected abstract TradfriCoapResource parsePayload(String coapPayload) throws JsonSyntaxException;

    protected void updateData(TradfriCoapResource data) {
        final TradfriCoapResource old = this.cachedData;
        this.cachedData = data;
        this.onUpdate(old, data);
        this.resourceCache.updated(this);
    }

    protected void onUpdate(TradfriCoapResource oldData, TradfriCoapResource newData) {
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
}