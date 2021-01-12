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

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapGroup;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.openhab.binding.tradfri.internal.model.TradfriGroup;
import org.openhab.binding.tradfri.internal.model.TradfriScene;

/**
 * {@link TradfriCoapGroupProxy} observes changes of a single group and related scenes
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapGroupProxy extends TradfriCoapThingResourceProxy implements TradfriGroup {

    private @Nullable TradfriResourceListObserver sceneListObserver;

    public TradfriCoapGroupProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient,
            ScheduledExecutorService scheduler) {
        super(resourceCache, THING_TYPE_GROUP, coapClient, scheduler);

        String sceneListUri = coapClient.getURI().replaceFirst(ENDPOINT_GROUPS, ENDPOINT_SCENES);
        this.sceneListObserver = new TradfriResourceListObserver(sceneListUri, coapClient.getEndpoint(), scheduler);
        this.sceneListObserver.registerHandler(this::handleSceneListChange);
    }

    @Override
    public Optional<TradfriScene> getSceneById(String id) {
        return getResourceCache().getAs(id, TradfriScene.class);
    }

    @Override
    public Optional<TradfriScene> getActiveScene() {
        return getDataAs(TradfriCoapGroup.class).flatMap(group -> group.getSceneId().flatMap(id -> getSceneById(id)));
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
        if (event.is(EType.RESOURCE_ADDED)) {
            getInstanceId().ifPresent(groudId -> getResourceCache().createSceneProxy(groudId, event.getId()));
        } else if (event.is(EType.RESOURCE_REMOVED)) {
            // Remove scene proxy from resource cache
            getResourceCache().remove(event.getId()).ifPresent(proxy -> proxy.dispose());
        }
    }

}
