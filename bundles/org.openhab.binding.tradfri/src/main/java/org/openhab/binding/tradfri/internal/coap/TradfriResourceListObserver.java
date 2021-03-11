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
import java.util.concurrent.ScheduledFuture;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/**
 * {@link TradfriResourceListObserver} observes a list of resources of the gateway
 * and generates events for the creation and deletion of devices, groups or scenes.
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriResourceListObserver implements CoapHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Gson gson = new Gson();

    private final TradfriCoapClient coapClient;
    private final String coapPath;

    private @Nullable ScheduledFuture<?> updateJob;

    private final Set<TradfriResourceListEventHandler> updateHandler = new CopyOnWriteArraySet<>();

    // Stores resource IDs
    private Set<String> cachedResources = Collections.emptySet();

    private int updateCounter = 0;

    private int POLL_PERIOD = 60;

    public TradfriResourceListObserver(TradfriCoapClient coapClient, String coapPath) {
        this.coapClient = coapClient;
        this.coapPath = coapPath;
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
            this.updateJob = this.coapClient.poll(coapPath, this, POLL_PERIOD);
        }
    }

    public void triggerUpdate() {
        this.coapClient.get(coapPath, this);
    }

    @Override
    public void onLoad(@Nullable CoapResponse response) {
        if (response == null) {
            logger.trace("Received empty CoAP response.");
            return;
        }
        logger.trace("Processing CoAP response. Options: {}  Payload: {}", response.getOptions(),
                response.getResponseText());
        if (response.isSuccess()) {
            try {
                Type setType = new TypeToken<Set<String>>() {
                }.getType();
                onUpdate(gson.fromJson(response.getResponseText(), setType));
            } catch (JsonParseException e) {
                logger.error("Coap response is no valid json: {}, {}", response.getResponseText(), e.getMessage());
            }
        } else {
            logger.debug("CoAP error: '{}' '{}'  Options: {}  Payload: {}", response.getCode(),
                    response.getCode().name(), response.getOptions(), response.getResponseText());
        }
    }

    @Override
    public synchronized void onError() {
        logger.warn("CoAP error. Failed to get resource list update for {}.", this.coapPath);
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

    private synchronized void onUpdate(Set<String> currentResources) {
        // Inform listener about added resources
        currentResources.stream().filter(id -> !cachedResources.contains(id)).forEach(id -> updateHandler
                .forEach(listener -> listener.onUpdate(TradfriEvent.from(id, EType.RESOURCE_ADDED))));

        // Inform listener about removed resources
        cachedResources.stream().filter(id -> !currentResources.contains(id)).forEach(id -> updateHandler
                .forEach(listener -> listener.onUpdate(TradfriEvent.from(id, EType.RESOURCE_REMOVED))));

        this.cachedResources = currentResources;
        updateCounter++;
    }

}
