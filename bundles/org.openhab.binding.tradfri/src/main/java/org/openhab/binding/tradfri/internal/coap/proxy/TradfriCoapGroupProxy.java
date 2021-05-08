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

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.*;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapClient;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapResourceCache;
import org.openhab.binding.tradfri.internal.coap.TradfriResourceListObserver;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapGroup;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapGroupCmd;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapResource;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.openhab.binding.tradfri.internal.model.TradfriEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriGroup;
import org.openhab.binding.tradfri.internal.model.TradfriLight;
import org.openhab.binding.tradfri.internal.model.TradfriScene;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;

/**
 * {@link TradfriCoapGroupProxy} observes changes of a single group and related scenes
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapGroupProxy extends TradfriCoapThingResourceProxy implements TradfriEventHandler, TradfriGroup {

    private @Nullable TradfriResourceListObserver sceneListObserver;

    public TradfriCoapGroupProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient, String coapPath,
            String coapPayload) {
        super(resourceCache, coapClient, coapPath, dtoFrom(coapPayload, TradfriCoapGroup.class), THING_TYPE_GROUP);

        this.sceneListObserver = new TradfriResourceListObserver(coapClient,
                ENDPOINT_SCENES + "/" + getInstanceId().get());
        this.sceneListObserver.registerHandler(this::handleSceneListChange);
    }

    @Override
    public void initialize() {
        super.initialize();
        // Subscribe update event of assigned devices
        getDeviceIds().stream().forEach(id -> getResourceCache().subscribeEvents(id, EType.RESOURCE_UPDATED, this));
    }

    @Override
    protected void onUpdate(TradfriCoapResource oldData, TradfriCoapResource newData) {
        final Set<String> cachedDevices = getDeviceIds(oldData);
        final Set<String> currentDevices = getDeviceIds(newData);
        // Subscribe update event of added devices
        currentDevices.stream().filter(id -> !cachedDevices.contains(id))
                .forEach(id -> getResourceCache().subscribeEvents(id, EType.RESOURCE_UPDATED, this));
        // Unsubscribe update event of removed devices
        cachedDevices.stream().filter(id -> !currentDevices.contains(id))
                .forEach(id -> getResourceCache().unsubscribeEvents(id, EType.RESOURCE_UPDATED, this));
    }

    @Override
    public void onEvent(TradfriEvent event) {
        // TODO: trigger only if light bulbs changed
        if (event.is(EType.RESOURCE_UPDATED)) {
            getResourceCache().updated(this);
        }
    }

    @Override
    public boolean lightsOn() {
        return getLightsAreOn().count() > 0;
    }

    @Override
    public boolean lightsOff() {
        return !this.lightsOn();
    }

    @Override
    public void setOnOff(OnOffType value) {
        execute(new TradfriCoapGroupCmd(this).setOnOff(value == OnOffType.ON ? 1 : 0));
    }

    @Override
    public PercentType getBrightness() {
        return new PercentType((int) Math.round(getLights().filter(light -> light.isAlive() && light.isOn())
                .mapToInt(light -> light.getBrightness().intValue()).average().orElse(0)));
    }

    @Override
    public void setBrightness(PercentType value) {
        setBrightness(convertToAbsoluteBrightness(value));
    }

    @Override
    public void increaseBrightnessBy(PercentType value) {
        getLightsAreOn().forEach(light -> light.increaseBrightnessBy(value));
    }

    @Override
    public void decreaseBrightnessBy(PercentType value) {
        getLightsAreOn().forEach(light -> light.decreaseBrightnessBy(value));
    }

    @Override
    public Optional<TradfriScene> getSceneById(String id) {
        return getResourceCache().getAs(id, TradfriScene.class);
    }

    @Override
    public Optional<TradfriScene> getSceneByName(String name) {
        return getResourceCache()
                .streamOf(TradfriScene.class,
                        s -> s.getGroupID().equals(getInstanceId().get()) && name.equals(s.getSceneName().get()))
                .findFirst();
    }

    @Override
    public Optional<TradfriScene> getActiveScene() {
        return getDataAs(TradfriCoapGroup.class).flatMap(group -> group.getSceneId().flatMap(id -> getSceneById(id)));
    }

    @Override
    public void setActiveScene(TradfriScene scene) {
        scene.getInstanceId()
                .ifPresent(sceneID -> execute(new TradfriCoapGroupCmd(this).setOnOff(1).setScene(sceneID)));
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
        return dtoFrom(coapPayload, TradfriCoapGroup.class);
    }

    private synchronized void handleSceneListChange(TradfriEvent event) {
        if (event.is(EType.RESOURCE_ADDED)) {
            getInstanceId().ifPresent(groudId -> getResourceCache().createAndAddSceneProxy(groudId, event.getId()));
        } else if (event.is(EType.RESOURCE_REMOVED)) {
            // Remove scene proxy from resource cache
            getResourceCache().remove(event.getId()).ifPresent(proxy -> proxy.dispose());
        }
    }

    private void setBrightness(int value) {
        execute(new TradfriCoapGroupCmd(this).setDimmer(value));
    }

    private int convertToAbsoluteBrightness(PercentType relativeBrightness) {
        return (int) Math.floor(relativeBrightness.doubleValue() * 2.54);
    }

    private Stream<TradfriLight> getLightsAreOn() {
        return getLights().filter(light -> light.isAlive() && light.isOn());
    }

    private Stream<TradfriLight> getLights() {
        return getDeviceIds().stream().map(id -> getResourceCache().getAs(id, TradfriLight.class))
                .filter(Optional::isPresent).map(light -> light.get());
    }

    private Set<String> getDeviceIds() {
        return getDeviceIds(getData());
    }

    private Set<String> getDeviceIds(TradfriCoapResource groupData) {
        return groupData.as(TradfriCoapGroup.class).map(group -> group.getMembers().toSet())
                .orElse(Collections.emptySet());
    }
}
