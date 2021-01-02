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

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEventHandler;
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

    private final ConcurrentHashMap<String, TradfriCoapResourceProxy> proxyMap;

    private final ConcurrentHashMap<TradfriEvent, Set<WeakReference<Object>>> eventHandlerMap;

    private @Nullable TradfriCoapProxyFactory proxyFactory;

    public TradfriCoapResourceCache() {

        this.proxyMap = new ConcurrentHashMap<String, TradfriCoapResourceProxy>();
        this.eventHandlerMap = new ConcurrentHashMap<TradfriEvent, Set<WeakReference<Object>>>(
                TradfriEvent.values().length);
    }

    public boolean isInitialized() {
        TradfriResourceListObserver deviceListObserver = this.deviceListObserver;
        TradfriResourceListObserver groupListObserver = this.groupListObserver;
        boolean observerInitialzed = false;
        if (deviceListObserver != null && groupListObserver != null) {
            observerInitialzed = deviceListObserver.isInitialized() && groupListObserver.isInitialized();
        }

        return observerInitialzed;
    }

    public void initialize(String baseUri, Endpoint endpoint, ScheduledExecutorService scheduler) {
        this.proxyFactory = new TradfriCoapProxyFactory(this, baseUri, endpoint, scheduler);

        initializeResourceListObserver(baseUri, endpoint, scheduler);
    }

    @Override
    public void subscribeEvent(TradfriEvent event, Object subscriber) {
        if (!eventHandlerMap.containsKey(event)) {
            eventHandlerMap.put(event, new CopyOnWriteArraySet<WeakReference<Object>>());
        }
        eventHandlerMap.get(event).add(new WeakReference<>(subscriber));
    }

    @Override
    public boolean contains(String id) {
        return this.proxyMap.containsKey(id);
    }

    @Override
    public @Nullable TradfriCoapResourceProxy get(String id) {
        return this.proxyMap.get(id);
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

        this.eventHandlerMap.values().parallelStream().forEach((handlers) -> handlers.clear());
        this.eventHandlerMap.clear();

        this.proxyMap.clear();
    }

    public void createDeviceProxy(String id) {
        if (!contains(id)) {
            if (this.proxyFactory != null) {
                /**
                 * Create new proxy for added device
                 * New proxy adds itself automatically to the resource storage
                 */
                this.proxyFactory.createDeviceProxy(id);
            } else {
                logger.error("Unexpected initializaion error. Device with ID {} couldn't be added.", id);
            }
        }
    }

    public void createGroupProxy(String id) {
        if (!contains(id)) {
            if (this.proxyFactory != null) {
                /**
                 * Create new proxy for added group
                 * New proxy adds itself automatically to the resource storage
                 */
                this.proxyFactory.createGroupProxy(id);
            } else {
                logger.error("Unexpected initializaion error. Group with ID {} couldn't be added.", id);
            }
        }
    }

    public void createSceneProxy(String groupId, String sceneId) {
        if (this.proxyFactory != null) {
            this.proxyFactory.createSceneProxy(groupId, sceneId);
        } else {
            // TODO: log error message
        }
    }

    public void add(TradfriCoapResourceProxy proxy) {
        String id = proxy.getInstanceId();

        if (id == null) {
            throw new IllegalArgumentException("Instance ID of argument 'proxy' must not be null");
        }

        if (!this.proxyMap.containsKey(id)) {
            this.proxyMap.put(id, proxy);
            publish(TradfriEvent.RESOURCE_ADDED, proxy);
        } else {
            // TODO error handling
        }
    }

    public void updated(TradfriResource proxy) {
        publish(TradfriEvent.RESOURCE_UPDATED, proxy);
    }

    public @Nullable TradfriCoapResourceProxy remove(String id) {
        TradfriCoapResourceProxy proxy = get(id);
        if (proxy != null) {
            this.proxyMap.remove(id);
            publish(TradfriEvent.RESOURCE_REMOVED, proxy);
        }
        return proxy;
    }

    private void publish(TradfriEvent event, TradfriResource resource) {
        eventHandlerMap.get(event).parallelStream().forEach((subscriberRef) -> {
            Object subscriberObj = subscriberRef.get();

            for (final Method method : subscriberObj.getClass().getDeclaredMethods()) {
                TradfriEventHandler annotation = method.getAnnotation(TradfriEventHandler.class);
                if (annotation != null && event == annotation.value()) {
                    deliverEvent(subscriberObj, method, resource);
                }
            }
        });
    }

    private <T, R extends TradfriResource> void deliverEvent(T subscriber, Method method, R resource) {
        try {
            boolean methodFound = false;
            for (final Class<?> paramClass : method.getParameterTypes()) {
                if (paramClass.isAssignableFrom(resource.getClass())) {
                    methodFound = true;
                    break;
                }
            }
            if (methodFound) {
                method.setAccessible(true);
                method.invoke(subscriber, resource);
            }
        } catch (Exception e) {
            // TODO error handling
        }
    }

    /**
     * Initialize all observer to requests the lists of available devices and groups.
     * Hint: The native CoAP observe mechanism is currently not supported by the TRADFRI gateway
     * for lists of devices and groups. Therefore the ResourceListObserver are polling
     * the gateway every 60 seconds for changes.
     */
    private void initializeResourceListObserver(String baseUri, Endpoint endpoint, ScheduledExecutorService scheduler) {
        // Create observer for devices, groups and scenes and observe lists

        TradfriResourceListObserver deviceListObserver = new TradfriResourceListObserver(
                baseUri + "/" + ENDPOINT_DEVICES, endpoint, scheduler);
        this.deviceListObserver = deviceListObserver;
        deviceListObserver.registerHandler(this::handleDeviceListChange);
        deviceListObserver.observe();

        TradfriResourceListObserver groupListObserver = new TradfriResourceListObserver(baseUri + "/" + ENDPOINT_GROUPS,
                endpoint, scheduler);
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

    private synchronized void handleDeviceListChange(TradfriEvent event, String id) {
        // A device was added.
        if (event == TradfriEvent.RESOURCE_ADDED) {
            createDeviceProxy(id);
            // A device was removed
        } else if (event == TradfriEvent.RESOURCE_REMOVED) {
            // Remove proxy of removed device
            TradfriResource proxy = remove(id);
            // TODO: error handling if there is a configured thing for that proxy
            if (proxy != null) {
                // Destroy proxy object
                proxy.dispose();
            }
        }
    }

    private synchronized void handleGroupListChange(TradfriEvent event, String id) {
        // A group was added
        if (event == TradfriEvent.RESOURCE_ADDED) {
            createGroupProxy(id);
            // A group was removed
        } else if (event == TradfriEvent.RESOURCE_REMOVED) {
            // Remove proxy of removed group
            TradfriResource proxy = remove(id);
            // TODO: error handling if there is a configured thing for that proxy
            if (proxy != null) {
                // Destroy proxy object
                proxy.dispose();
            }
        }
    }
}
