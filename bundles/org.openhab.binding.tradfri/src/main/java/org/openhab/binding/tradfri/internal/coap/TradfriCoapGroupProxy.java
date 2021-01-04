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

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapGroup;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.openhab.binding.tradfri.internal.model.TradfriGroup;
import org.openhab.binding.tradfri.internal.model.TradfriResource;
import org.openhab.binding.tradfri.internal.model.TradfriScene;

/**
 * {@link TradfriCoapGroupProxy} observes changes of a single group and related scenes
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapGroupProxy extends TradfriCoapResourceProxy implements TradfriGroup {

    private @Nullable TradfriResourceListObserver sceneListObserver;

    public TradfriCoapGroupProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient,
            ScheduledExecutorService scheduler) {
        super(resourceCache, coapClient, scheduler);

        String sceneListUri = coapClient.getURI().replaceFirst(ENDPOINT_GROUPS, ENDPOINT_SCENES);
        this.sceneListObserver = new TradfriResourceListObserver(sceneListUri, coapClient.getEndpoint(), scheduler);
        this.sceneListObserver.registerHandler(this::handleSceneListChange);
    }

    @Override
    public @Nullable TradfriScene getSceneById(String id) {
        TradfriResource proxy = this.resourceCache.get(id);
        return (proxy != null && proxy instanceof TradfriScene) ? (TradfriScene) proxy : null;
    }

    @Override
    public @Nullable TradfriScene getActiveScene() {
        TradfriScene sceneProxy = null;

        TradfriCoapGroup groupData = (TradfriCoapGroup) this.cachedData;
        if (groupData != null) {
            String sceneId = groupData.getSceneId();
            if (sceneId != null) {
                sceneProxy = getSceneById(sceneId);
            }
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
    }

    @Override
    public TradfriCoapGroup parsePayload(String coapPayload) {
        return gson.fromJson(coapPayload, TradfriCoapGroup.class);
    }

    private synchronized void handleSceneListChange(TradfriEvent event) {
        final String sceneId = event.getId();
        if (event.is(EType.RESOURCE_ADDED)) {
            final String groupId = getInstanceId();
            if (groupId != null && sceneId != null) {
                this.resourceCache.createSceneProxy(groupId, sceneId);
            }
        } else if (event.is(EType.RESOURCE_REMOVED)) {
            if (sceneId != null) {
                // Remove proxy of removed device
                TradfriCoapResourceProxy proxy = this.resourceCache.remove(sceneId);
                if (proxy != null) {
                    // Destroy proxy object
                    proxy.dispose();
                }
            }
        }
    }

}
