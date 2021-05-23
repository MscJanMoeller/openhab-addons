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

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.*;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapResourceProxy;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.openhab.binding.tradfri.internal.model.TradfriEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriEventSubscription;
import org.openhab.binding.tradfri.internal.model.TradfriResource;
import org.openhab.binding.tradfri.internal.model.TradfriResourceCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link TradfriCoapResourceCache} stores all proxy objects of specific
 * single resources like a device, group or scene.
 *
 * @author Jan Möller - Initial contribution
 *
 */

@NonNullByDefault
public class TradfriCoapResourceCache implements TradfriResourceCache {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private @Nullable TradfriResourceListObserver deviceListObserver;
    private @Nullable TradfriResourceListObserver groupListObserver;

    private final Map<String, TradfriCoapResourceProxy> proxyMap;

    private final Map<TradfriEventSubscription, Set<TradfriEventHandler>> eventHandlerMap;

    private @Nullable TradfriCoapProxyFactory proxyFactory;

    public TradfriCoapResourceCache() {
        this.proxyMap = new ConcurrentHashMap<String, TradfriCoapResourceProxy>();
        this.eventHandlerMap = new ConcurrentHashMap<TradfriEventSubscription, Set<TradfriEventHandler>>();
    }

    public boolean isInitialized() {
        final TradfriResourceListObserver deviceListObserver = this.deviceListObserver;
        final TradfriResourceListObserver groupListObserver = this.groupListObserver;
        boolean observerInitialzed = false;
        if (deviceListObserver != null && groupListObserver != null) {
            observerInitialzed = deviceListObserver.isInitialized() && groupListObserver.isInitialized();
        }

        return observerInitialzed;
    }

    public void initialize(final TradfriCoapClient coapClient, final ScheduledExecutorService scheduler) {
        this.proxyFactory = new TradfriCoapProxyFactory(this, coapClient);

        initializeResourceListObserver(coapClient, scheduler);
    }

    @Override
    public void subscribeEvents(TradfriEventHandler subscriber) {
        subscribe(TradfriEventSubscription.allEvents(), subscriber);
    }

    @Override
    public void subscribeEvents(String id, TradfriEventHandler subscriber) {
        subscribe(new TradfriEventSubscription(id), subscriber);
    }

    @Override
    public void subscribeEvents(EnumSet<EType> eventTypes, TradfriEventHandler subscriber) {
        subscribe(new TradfriEventSubscription(eventTypes), subscriber);
    }

    @Override
    public void subscribeEvents(String id, EType eventType, TradfriEventHandler subscriber) {
        subscribe(new TradfriEventSubscription(id, EnumSet.of(eventType)), subscriber);
    }

    @Override
    public void subscribeEvents(String id, EnumSet<EType> eventTypes, TradfriEventHandler subscriber) {
        subscribe(new TradfriEventSubscription(id, eventTypes), subscriber);
    }

    @Override
    public void unsubscribeEvents(TradfriEventHandler subscriber) {
        this.eventHandlerMap.entrySet().forEach((entry) -> {
            if (entry.getValue().remove(subscriber)) {
                if (entry.getValue().isEmpty()) {
                    this.eventHandlerMap.remove(entry.getKey());
                }
            }
        });
    }

    @Override
    public void unsubscribeEvents(String id, EType eventType, TradfriEventHandler subscriber) {
        this.eventHandlerMap.entrySet().forEach((entry) -> {
            if (entry.getKey().covers(id, eventType)) {
                if (entry.getValue().remove(subscriber)) {
                    if (entry.getValue().isEmpty()) {
                        this.eventHandlerMap.remove(entry.getKey());
                    }
                }
            }
        });
    }

    @Override
    public boolean contains(String id) {
        return this.proxyMap.containsKey(id);
    }

    @Override
    public Optional<TradfriCoapResourceProxy> get(String id) {
        logger.trace("Resource with id {} requested", id);
        return Optional.ofNullable(this.proxyMap.get(id));
    }

    @Override
    public <T extends TradfriResource> Optional<T> getAs(String id, Class<T> resourceClass) {
        logger.trace("Resource with id {} as {} requested", id, resourceClass);
        return get(id).flatMap(resource -> resource.as(resourceClass));
    }

    @Override
    public <T extends TradfriResource> Stream<T> streamOf(Class<T> resourceClass) {
        return this.proxyMap.values().parallelStream().filter(r -> resourceClass.isAssignableFrom(r.getClass()))
                .map(r -> resourceClass.cast(r));
    }

    @Override
    public <T extends TradfriResource> Stream<T> streamOf(Class<T> resourceClass, Predicate<T> predicate) {
        return streamOf(resourceClass).filter(predicate);
    }

    @Override
    public void refresh() {
        if (this.deviceListObserver != null) {
            this.deviceListObserver.triggerUpdate();
        }
        if (this.groupListObserver != null) {
            this.groupListObserver.triggerUpdate();
        }

        this.proxyMap.values().parallelStream().forEach((proxy) -> proxy.triggerUpdate());
    }

    @Override
    public void clear() {
        // Implicitly disposes all resources by triggering RESOURCE_REMOVED event
        disposeResourceListObserver();

        this.eventHandlerMap.values().parallelStream().forEach((subscribers) -> subscribers.clear());
        this.eventHandlerMap.clear();

        this.proxyMap.clear();
    }

    public void createAndAddDeviceProxy(String id) {
        if (!contains(id)) {
            if (this.proxyFactory != null) {
                /**
                 * Create new proxy for added device
                 * New proxy adds itself automatically to the resource cache
                 */
                this.proxyFactory.createAndAddDeviceProxy(id);
            } else {
                logger.error("Unexpected initializaion error. Device with id {} couldn't be added.", id);
            }
        }
    }

    public void createAndAddGroupProxy(String id) {
        if (!contains(id)) {
            if (this.proxyFactory != null) {
                /**
                 * Create new proxy for added group
                 * New proxy adds itself automatically to the resource cache
                 */
                this.proxyFactory.createAndAddGroupProxy(id);
            } else {
                logger.error("Unexpected initializaion error. Group with id {} couldn't be added.", id);
            }
        }
    }

    public void createAndAddSceneProxy(String groupId, String sceneId) {
        if (!contains(sceneId)) {
            if (this.proxyFactory != null) {
                /**
                 * Create new proxy for added scene
                 * New proxy adds itself automatically to the resource storage
                 */
                this.proxyFactory.createAndAddSceneProxy(groupId, sceneId);
            } else {
                logger.error("Unexpected initializaion error. Scene with id {} couldn't be added.", sceneId);
            }
        }
    }

    public TradfriCoapResourceProxy add(TradfriCoapResourceProxy proxy) {
        proxy.getInstanceId().ifPresent(id -> {
            if (!contains(id)) {
                this.proxyMap.put(id, proxy);
                logger.trace("Added resource with id {} to resource cache.", id);
                publish(EType.RESOURCE_ADDED, proxy);
            }
        });
        return proxy;
    }

    public void updated(TradfriCoapResourceProxy proxy) {
        publish(EType.RESOURCE_UPDATED, proxy);
    }

    public Optional<TradfriCoapResourceProxy> remove(String id) {
        final Optional<TradfriCoapResourceProxy> proxy = get(id);
        if (proxy.isPresent()) {
            publish(EType.RESOURCE_REMOVED, proxy.get());
            this.proxyMap.remove(id);
        }
        return proxy;
    }

    private void subscribe(TradfriEventSubscription eventSubscription, TradfriEventHandler subscriber) {
        if (!eventHandlerMap.containsKey(eventSubscription)) {
            eventHandlerMap.put(eventSubscription, new CopyOnWriteArraySet<TradfriEventHandler>());
        }
        eventHandlerMap.get(eventSubscription).add(subscriber);
        logger.trace("Added event subscription for {}", eventSubscription);
    }

    private void publish(EType eventType, TradfriCoapResourceProxy proxy) {
        proxy.getInstanceId().ifPresent(id -> {
            final TradfriEvent event = TradfriEvent.from(id, eventType);
            this.eventHandlerMap.entrySet().forEach((entry) -> {
                if (entry.getKey().covers(event)) {
                    entry.getValue().parallelStream().forEach((subscriber) -> subscriber.onEvent(event));
                }
            });
        });
    }

    /**
     * Initialize all observer to requests the lists of available devices and groups.
     * Hint: The native CoAP observe mechanism is currently not supported by the TRADFRI gateway
     * for lists of devices and groups. Therefore the ResourceListObserver are polling
     * the gateway every 60 seconds for changes.
     */
    private void initializeResourceListObserver(final TradfriCoapClient coapClient,
            final ScheduledExecutorService scheduler) {
        // Create observer for devices, groups and scenes and observe lists

        TradfriResourceListObserver deviceListObserver = new TradfriResourceListObserver(coapClient, ENDPOINT_DEVICES);
        this.deviceListObserver = deviceListObserver;
        deviceListObserver.registerHandler(this::handleDeviceListChange);
        deviceListObserver.observe();

        TradfriResourceListObserver groupListObserver = new TradfriResourceListObserver(coapClient, ENDPOINT_GROUPS);
        this.groupListObserver = groupListObserver;
        groupListObserver.registerHandler(this::handleGroupListChange);
        groupListObserver.observe();
    }

    private void disposeResourceListObserver() {
        if (this.deviceListObserver != null) {
            // Triggers RESOURCE_REMOVED event for all resources
            this.deviceListObserver.dispose();
            this.deviceListObserver = null;
        }
        if (this.groupListObserver != null) {
            // Triggers RESOURCE_REMOVED event for all resources
            this.groupListObserver.dispose();
            this.groupListObserver = null;
        }
    }

    private void handleDeviceListChange(TradfriEvent event) {
        final String id = event.getId();
        // A device was added.
        if (event.is(EType.RESOURCE_ADDED)) {
            createAndAddDeviceProxy(id);
            // A device was removed
        } else if (event.is(EType.RESOURCE_REMOVED)) {
            // Remove proxy of removed device
            remove(id).ifPresent(proxy -> proxy.dispose());
            // TODO: error handling if there is a configured thing for that proxy
        }
    }

    private void handleGroupListChange(TradfriEvent event) {
        final String id = event.getId();
        // A group was added
        if (event.is(EType.RESOURCE_ADDED)) {
            createAndAddGroupProxy(id);
            // A group was removed
        } else if (event.is(EType.RESOURCE_REMOVED)) {
            // Remove proxy of removed group
            remove(id).ifPresent(proxy -> proxy.dispose());
            // TODO: error handling if there is a configured thing for that proxy
        }
    }
}