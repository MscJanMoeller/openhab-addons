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

package org.openhab.binding.tradfri.internal.coap;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.openhab.binding.tradfri.internal.model.TradfriEventHandler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * {@link TradfriResourceListObserver} observes a list of resources of the gateway
 * and generates events for the creation and deletion of devices, groups or scenes.
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriResourceListObserver {
    private static final Gson GSON = new Gson();

    private final TradfriCoapClient coapClient;
    private final String coapPath;

    private @Nullable ScheduledFuture<?> updateJob;

    private final Set<TradfriEventHandler> eventHandler = new CopyOnWriteArraySet<>();

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
            this.updateJob = this.coapClient.poll(coapPath, POLL_PERIOD, this::processResponse);
        }
    }

    public void triggerUpdate() {
        this.coapClient.get(coapPath, this::processResponse);
    }

    public void processResponse(String payload) {
        Type setType = new TypeToken<Set<String>>() {
        }.getType();
        onUpdate(GSON.fromJson(payload, setType));
    }

    public synchronized void dispose() {
        updateCounter = 0;

        this.cachedResources.forEach(
                id -> eventHandler.forEach(handler -> handler.onEvent(TradfriEvent.from(id, EType.RESOURCE_REMOVED))));

        this.cachedResources.clear();
        this.eventHandler.clear();

        if (this.updateJob != null) {
            this.updateJob.cancel(true);
            this.updateJob = null;
        }
    }

    /**
     * Registers a handler, which will be informed about resource events.
     *
     * @param handler the handler to register
     */
    public void registerHandler(TradfriEventHandler handler) {
        this.eventHandler.add(handler);
    }

    /**
     * Unregisters a given handler.
     *
     * @param handler the handler to unregister
     */
    public void unregisterHandler(TradfriEventHandler handler) {
        this.eventHandler.remove(handler);
    }

    private synchronized void onUpdate(@Nullable Set<String> resourceList) {
        final Set<String> currentResources = resourceList != null ? resourceList : Collections.emptySet();

        // Inform listener about added resources
        currentResources.stream().filter(id -> !cachedResources.contains(id)).forEach(
                id -> eventHandler.forEach(handler -> handler.onEvent(TradfriEvent.from(id, EType.RESOURCE_ADDED))));

        // Inform listener about removed resources
        cachedResources.stream().filter(id -> !currentResources.contains(id)).forEach(
                id -> eventHandler.forEach(handler -> handler.onEvent(TradfriEvent.from(id, EType.RESOURCE_REMOVED))));

        this.cachedResources = currentResources;
        updateCounter++;
    }
}
