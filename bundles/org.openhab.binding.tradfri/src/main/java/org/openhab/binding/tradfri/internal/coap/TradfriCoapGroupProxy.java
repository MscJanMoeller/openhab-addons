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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapGroup;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapResource;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.openhab.binding.tradfri.internal.model.TradfriGroup;
import org.openhab.binding.tradfri.internal.model.TradfriScene;

import com.google.gson.JsonObject;

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
            ScheduledExecutorService scheduler, JsonObject coapPayload) {
        super(resourceCache, coapClient, scheduler, gson.fromJson(coapPayload, TradfriCoapGroup.class),
                THING_TYPE_GROUP);

        String sceneListUri = coapClient.getURI().replaceFirst(ENDPOINT_GROUPS, ENDPOINT_SCENES);
        this.sceneListObserver = new TradfriResourceListObserver(sceneListUri, coapClient.getEndpoint(), scheduler);
        this.sceneListObserver.registerHandler(this::handleSceneListChange);
    }

    @Override
    protected void onUpdate(TradfriCoapResource oldData, TradfriCoapResource newData) {
        final Set<String> cachedDevices = oldData.as(TradfriCoapGroup.class).map(group -> group.getMembers().toSet())
                .orElse(Collections.emptySet());
        final Set<String> currentDevices = newData.as(TradfriCoapGroup.class).map(group -> group.getMembers().toSet())
                .orElse(Collections.emptySet());
        // Subscribe update event of added devices
        currentDevices.stream().filter(id -> !cachedDevices.contains(id))
                .forEach(id -> getResourceCache().subscribeEvents(id, EType.RESOURCE_UPDATED, this));
        // Unsubscribe update event of removed devices
        cachedDevices.stream().filter(id -> !currentDevices.contains(id))
                .forEach(id -> getResourceCache().unsubscribeEvents(id, EType.RESOURCE_UPDATED, this));
    }

    @Override
    public boolean isOn() {
        // TODO: implement
        return false;
    }

    @Override
    public boolean isOff() {
        return !this.isOn();
    }

    @Override
    public void setOnOff(OnOffType value) {
        // TODO Auto-generated method stub
    }

    @Override
    public PercentType getBrightness() {
        // TODO: implement
        return PercentType.ZERO;
    }

    @Override
    public void setBrightness(PercentType value) {
        // TODO: implement setBrightness(convertToAbsoluteBrightness(value));
    }

    @Override
    public void increaseBrightnessBy(PercentType value) {
        // TODO: implement setBrightness(Math.min(getDimmer() + convertToAbsoluteBrightness(value), 254));
    }

    @Override
    public void decreaseBrightnessBy(PercentType value) {
        // TODO: implement setBrightness(Math.max(getDimmer() - convertToAbsoluteBrightness(value), 0));
    }

    @Override
    public Optional<TradfriScene> getActiveScene() {
        return getDataAs(TradfriCoapGroup.class).flatMap(group -> group.getSceneId().flatMap(id -> getSceneById(id)));
    }

    @Override
    public Optional<TradfriScene> getSceneById(String id) {
        return getResourceCache().getAs(id, TradfriScene.class);
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
    protected TradfriCoapGroup parsePayload(String coapPayload) {
        return gson.fromJson(coapPayload, TradfriCoapGroup.class);
    }

    private synchronized void handleSceneListChange(TradfriEvent event) {
        if (event.is(EType.RESOURCE_ADDED)) {
            getInstanceId().ifPresent(groudId -> getResourceCache().createAndAddSceneProxy(groudId, event.getId()));
        } else if (event.is(EType.RESOURCE_REMOVED)) {
            // Remove scene proxy from resource cache
            getResourceCache().remove(event.getId()).ifPresent(proxy -> proxy.dispose());
        }
    }
}
