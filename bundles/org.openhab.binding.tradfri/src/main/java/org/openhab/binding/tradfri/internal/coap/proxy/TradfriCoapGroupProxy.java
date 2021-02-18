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

package org.openhab.binding.tradfri.internal.coap.proxy;

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.*;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapClient;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapResourceCache;
import org.openhab.binding.tradfri.internal.coap.TradfriResourceListObserver;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapGroup;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapLightCmd;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapResource;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.openhab.binding.tradfri.internal.model.TradfriEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriGroup;
import org.openhab.binding.tradfri.internal.model.TradfriLight;
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

    public TradfriCoapGroupProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient, String coapPath,
            JsonObject coapPayload) {
        super(resourceCache, coapClient, coapPath, gson.fromJson(coapPayload, TradfriCoapGroup.class),
                THING_TYPE_GROUP);

        this.sceneListObserver = new TradfriResourceListObserver(coapClient,
                ENDPOINT_SCENES + "/" + getInstanceId().get());
        this.sceneListObserver.registerHandler(this::handleSceneListChange);
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

    @TradfriEventHandler(EType.RESOURCE_UPDATED)
    public void onDeviceUpdated(TradfriEvent event, TradfriCoapThingResourceProxy device) {
        // TODO: trigger only if light bulbs changed
        getResourceCache().updated(device);
    }

    @Override
    public boolean lightsOn() {
        return getLights().map(light -> light.isAlive() && light.isOn()).reduce(Boolean::logicalOr).orElse(false);
    }

    @Override
    public boolean lightsOff() {
        return !this.lightsOn();
    }

    @Override
    public void setOnOff(OnOffType value) {
        execute(new TradfriCoapLightCmd(this).setOnOff(value == OnOffType.ON ? 1 : 0));
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
        setBrightness(new PercentType(
                Math.min(getBrightness().intValue() + value.intValue(), PercentType.HUNDRED.intValue())));
    }

    @Override
    public void decreaseBrightnessBy(PercentType value) {
        setBrightness(
                new PercentType(Math.max(getBrightness().intValue() - value.intValue(), PercentType.ZERO.intValue())));
    }

    @Override
    public Optional<TradfriScene> getActiveScene() {
        return getDataAs(TradfriCoapGroup.class).flatMap(group -> group.getSceneId().flatMap(id -> getSceneById(id)));
    }

    @Override
    public void setActiveSceneByName(String name) {
        // TODO implement command
    }

    @Override
    public void setActiveSceneById(String id) {
        // TODO implement command
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

    private void setBrightness(int value) {
        execute(new TradfriCoapLightCmd(this).setDimmer(value));
    }

    private int convertToAbsoluteBrightness(PercentType relativeBrightness) {
        return (int) Math.floor(relativeBrightness.doubleValue() * 2.54);
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
