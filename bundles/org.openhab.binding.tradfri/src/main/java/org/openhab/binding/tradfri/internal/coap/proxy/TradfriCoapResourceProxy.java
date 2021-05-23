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

package org.openhab.binding.tradfri.internal.coap.proxy;

import java.util.Optional;

import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapClient;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapResourceCache;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapCmd;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapResource;
import org.openhab.binding.tradfri.internal.model.TradfriResource;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * {@link TradfriCoapResourceProxy} observes changes of a single
 * resource like a device, group or scene.
 *
 * @author Jan Möller - Initial contribution
 *
 */

@NonNullByDefault
public abstract class TradfriCoapResourceProxy implements TradfriResource {

    protected static final Gson GSON = new Gson();

    private final TradfriCoapClient coapClient;
    private final String coapPath;
    private @Nullable CoapObserveRelation observeRelation;

    private final TradfriCoapResourceCache resourceCache;

    private TradfriCoapResource cachedData;

    protected TradfriCoapResourceProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient,
            String coapPath, TradfriCoapResource initialData) {
        this.resourceCache = resourceCache;
        this.coapClient = coapClient;
        this.coapPath = coapPath;
        this.cachedData = initialData;
    }

    protected static <T extends TradfriCoapResource> T dtoFrom(String data, Class<T> resourceClass)
            throws IllegalArgumentException {
        final @Nullable T resourceData = GSON.fromJson(data, resourceClass);

        if (resourceData == null) {
            throw new IllegalArgumentException();
        }

        return resourceData;
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
        this.coapClient.get(coapPath, this::processResponse);
    }

    private void processResponse(String payload) {
        updateData(parsePayload(payload));
    }

    @Override
    public void dispose() {
        if (this.observeRelation != null) {
            this.observeRelation.reactiveCancel();
            this.observeRelation = null;
        }
    }

    protected TradfriCoapResourceCache getResourceCache() {
        return this.resourceCache;
    }

    protected TradfriCoapResource getData() {
        return this.cachedData;
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

    protected void execute(TradfriCoapCmd command) {
        this.coapClient.execute(command, this.coapPath);
    }

    protected synchronized void observe() {
        if (this.observeRelation != null) {
            this.observeRelation.reactiveCancel();
            this.observeRelation = null;
        }

        observeRelation = coapClient.observe(this.coapPath, this::processResponse);
    }
}