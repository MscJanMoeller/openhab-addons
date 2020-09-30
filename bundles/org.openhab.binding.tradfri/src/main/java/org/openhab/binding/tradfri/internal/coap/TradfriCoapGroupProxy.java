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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.coap.TradfriResourceListEventHandler.ResourceListEvent;
import org.openhab.binding.tradfri.internal.coap.status.TradfriGroup;
import org.openhab.binding.tradfri.internal.model.TradfriGroupProxy;
import org.openhab.binding.tradfri.internal.model.TradfriSceneProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

/**
 * {@link TradfriCoapGroupProxy} observes changes of a single group and related scenes
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapGroupProxy extends TradfriCoapResourceProxy implements TradfriGroupProxy {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final String gatewayUri;
    private final Endpoint endpoint;
    private final ScheduledExecutorService scheduler;

    private final @NonNullByDefault({}) Map<String, TradfriSceneProxy> sceneProxyMap;

    private @Nullable TradfriResourceListObserver sceneListObserver;

    public TradfriCoapGroupProxy(String gatewayUri, String groupId, Endpoint endpoint,
            ScheduledExecutorService scheduler) {
        super(gatewayUri + "/" + ENDPOINT_GROUPS + "/" + groupId, endpoint, scheduler);

        this.gatewayUri = gatewayUri;
        this.endpoint = endpoint;
        this.scheduler = scheduler;

        this.sceneProxyMap = new ConcurrentHashMap<String, TradfriSceneProxy>();

        this.sceneListObserver = new TradfriResourceListObserver(gatewayUri + "/" + ENDPOINT_SCENES + "/" + groupId,
                endpoint, scheduler);
        this.sceneListObserver.registerHandler(this::handleSceneListChange);
    }

    @Override
    public @Nullable TradfriSceneProxy getSceneById(@Nullable String id) {
        return sceneProxyMap.get(id);
    }

    @Override
    public @Nullable TradfriSceneProxy getActiveScene() {
        TradfriSceneProxy sceneProxy = null;

        TradfriGroup groupData = (TradfriGroup) this.cachedData;
        if (groupData != null) {
            sceneProxy = getSceneById(groupData.getSceneId());
        } else {
            logger.debug("Unexpected error. Proxy object of group not initialized yet");
        }

        return sceneProxy;
    }

    @Override
    public void dispose() {
        if (this.sceneListObserver != null) {
            this.sceneListObserver.dispose();
            this.sceneListObserver = null;
        }
        super.dispose();
    }

    @Override
    public void observe() {
        super.observe();
        if (this.sceneListObserver != null) {
            sceneListObserver.observe();
        }
    }

    @Override
    public void triggerUpdate() {
        super.observe();
        if (this.sceneListObserver != null) {
            sceneListObserver.triggerUpdate();
        }
        this.sceneProxyMap.forEach((id, proxy) -> proxy.triggerUpdate());
    }

    @Override
    protected TradfriGroup convert(JsonElement data) {
        return gson.fromJson(data, TradfriGroup.class);
    }

    private synchronized void handleSceneListChange(ResourceListEvent event, String id) {
        if (event == ResourceListEvent.RESOURCE_ADDED) {
            if (!this.sceneProxyMap.containsKey(id)) {
                // A scene was added. Create new proxy for that scene
                TradfriCoapSceneProxy proxy = new TradfriCoapSceneProxy(gatewayUri, id, endpoint, scheduler);
                // Add this proxy to the list of proxies
                this.sceneProxyMap.put(id, proxy);
                // Start observation of scene updates
                proxy.observe();
            }
        } else if (event == ResourceListEvent.RESOURCE_REMOVED) {
            if (this.sceneProxyMap.containsKey(id)) {
                // A scene was removed. Remove proxy of that scene
                TradfriCoapSceneProxy proxy = (TradfriCoapSceneProxy) this.sceneProxyMap.remove(id);
                // Destroy proxy
                proxy.dispose();
            }
        }
    }
}
