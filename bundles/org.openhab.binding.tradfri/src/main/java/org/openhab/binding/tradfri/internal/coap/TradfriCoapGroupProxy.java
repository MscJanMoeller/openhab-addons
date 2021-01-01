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
import org.openhab.binding.tradfri.internal.coap.status.TradfriGroup;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriGroupProxy;
import org.openhab.binding.tradfri.internal.model.TradfriResourceProxy;
import org.openhab.binding.tradfri.internal.model.TradfriSceneProxy;

/**
 * {@link TradfriCoapGroupProxy} observes changes of a single group and related scenes
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapGroupProxy extends TradfriCoapResourceProxy implements TradfriGroupProxy {

    private @Nullable TradfriResourceListObserver sceneListObserver;

    public TradfriCoapGroupProxy(TradfriCoapResourceStorage resourceStorage, TradfriCoapClient coapClient,
            ScheduledExecutorService scheduler) {
        super(resourceStorage, coapClient, scheduler);

        String sceneListUri = coapClient.getURI().replaceFirst(ENDPOINT_GROUPS, ENDPOINT_SCENES);
        this.sceneListObserver = new TradfriResourceListObserver(sceneListUri, coapClient.getEndpoint(), scheduler);
        this.sceneListObserver.registerHandler(this::handleSceneListChange);
    }

    @Override
    public @Nullable TradfriSceneProxy getSceneById(String id) {
        TradfriResourceProxy proxy = this.resourceStorage.get(id);
        return (proxy != null && proxy instanceof TradfriSceneProxy) ? (TradfriSceneProxy) proxy : null;
    }

    @Override
    public @Nullable TradfriSceneProxy getActiveScene() {
        TradfriSceneProxy sceneProxy = null;

        TradfriGroup groupData = (TradfriGroup) this.cachedData;
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
    public TradfriGroup parsePayload(String coapPayload) {
        return gson.fromJson(coapPayload, TradfriGroup.class);
    }

    private synchronized void handleSceneListChange(TradfriEvent event, String id) {
        if (event == TradfriEvent.RESOURCE_ADDED) {
            String groupId = getInstanceId();
            if (groupId != null) {
                this.resourceStorage.createSceneProxy(groupId, id);
            }
        } else if (event == TradfriEvent.RESOURCE_REMOVED) {
            // Remove proxy of removed device
            TradfriCoapResourceProxy proxy = this.resourceStorage.remove(id);
            if (proxy != null) {
                // Destroy proxy object
                proxy.dispose();
            }
        }
    }

}
