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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapColorLight;
import org.openhab.binding.tradfri.internal.model.TradfriColorLight;
import org.openhab.binding.tradfri.internal.model.TradfriColorTempLight;
import org.openhab.binding.tradfri.internal.model.TradfriDevice;
import org.openhab.binding.tradfri.internal.model.TradfriDimmableLight;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.openhab.binding.tradfri.internal.model.TradfriEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriResource;
import org.openhab.binding.tradfri.internal.model.TradfriThingResource;

import com.google.gson.Gson;

/**
 * Tests for {@link TradfriCoapResourceCache}.
 *
 * @author Jan Möller - Initial contribution
 */
public class TradfriCoapResourceCacheTest {

    private static final Gson gson = new Gson();

    @Mock
    private TradfriCoapClient coapClient;

    @Mock
    private ScheduledExecutorService scheduler;

    private TradfriCoapResourceCache resourceCache;

    private Queue<TradfriEvent> expectedEvents;
    private Queue<TradfriResource> expectedResources;

    private static TradfriCoapColorLight createTradfriCoapColorLight() {
        String json = "{\"3\":{\"0\":\"IKEA of Sweden\",\"6\":1,\"1\":\"TRADFRI bulb E27 CWS opal 600lm\","
                + "\"2\":\"\",\"3\":\"1.3.013\"},\"9001\":\"LR Floor Lamp\",\"9003\":65539,\"9002\":1538425240,"
                + "\"9020\":1603481417,\"9054\":0,\"9019\":1,\"5750\":2,\"3311\":[{\"5850\":1,\"5851\":254,\"5707\":1490,"
                + "\"5708\":61206,\"5709\":40632,\"5710\":22282,\"5706\":\"da5d41\",\"9003\":0}]}";

        return gson.fromJson(json, TradfriCoapColorLight.class);
    }

    @Before
    public void setUp() {
        initMocks(this);

        this.resourceCache = new TradfriCoapResourceCache();
        this.expectedEvents = new LinkedList<TradfriEvent>();
        this.expectedResources = new LinkedList<TradfriResource>();
    }

    @After
    public void cleanUp() {
        this.resourceCache.clear();
        this.expectedEvents.clear();
        this.expectedResources.clear();
    }

    @Test
    public void getLight() {
        final TradfriCoapResourceProxy device = new TradfriCoapColorLightProxy(this.resourceCache, this.coapClient,
                this.scheduler);

        final TradfriCoapColorLight bulbData = createTradfriCoapColorLight();
        final String resourceId = bulbData.getInstanceId().get();
        assertNotNull(resourceId);
        device.initialize(bulbData);

        final Optional<TradfriThingResource> asThingResource = this.resourceCache.getAs(resourceId,
                TradfriThingResource.class);
        assertThat(asThingResource.isPresent(), is(true));
        assertThat(asThingResource.get().getInstanceId().get(), is(resourceId));

        final Optional<TradfriDevice> asDevice = this.resourceCache.getAs(resourceId, TradfriDevice.class);
        assertThat(asDevice.isPresent(), is(true));
        assertThat(asDevice.get().getInstanceId().get(), is(resourceId));

        final Optional<TradfriDimmableLight> asDimmableLight = this.resourceCache.getAs(resourceId,
                TradfriDimmableLight.class);
        assertThat(asDimmableLight.isPresent(), is(true));
        assertThat(asDimmableLight.get().getInstanceId().get(), is(resourceId));

        final Optional<TradfriColorTempLight> asColorTempLight = this.resourceCache.getAs(resourceId,
                TradfriColorTempLight.class);
        assertThat(asColorTempLight.isPresent(), is(true));
        assertThat(asColorTempLight.get().getInstanceId().get(), is(resourceId));

        final Optional<TradfriColorLight> asColorLight = this.resourceCache.getAs(resourceId, TradfriColorLight.class);
        assertThat(asColorLight.isPresent(), is(true));
        assertThat(asColorLight.get().getInstanceId().get(), is(resourceId));
    }

    @Test
    public void subscribeAllEventsForDeviceWithoutIdAndType() {
        Object subscriber = new Object() {
            @TradfriEventHandler
            public void onAddedOrUpdatedOrRemoved(TradfriEvent event, TradfriDevice device) {
                expectedEvents.add(event);
                expectedResources.add(device);
            }
        };
        this.resourceCache.subscribeEvents(subscriber);

        final TradfriCoapResourceProxy device = new TradfriCoapColorLightProxy(this.resourceCache, this.coapClient,
                this.scheduler);

        final TradfriCoapColorLight actualBulbData = createTradfriCoapColorLight();
        final String actualResourceId = actualBulbData.getInstanceId().get();
        assertNotNull(actualResourceId);

        // Generates event RESOURCE_ADDED
        device.initialize(actualBulbData);
        // Generates event RESOURCE_UPDATED
        this.resourceCache.updated(device);
        // Generates event RESOURCE_REMOVED
        this.resourceCache.remove(actualResourceId);

        assertThat(expectedEvents.size(), is(3));
        assertThat(expectedEvents.remove().getType(), is(EType.RESOURCE_ADDED));
        assertThat(expectedEvents.remove().getType(), is(EType.RESOURCE_UPDATED));
        assertThat(expectedEvents.remove().getType(), is(EType.RESOURCE_REMOVED));

        assertThat(expectedResources.size(), is(3));
        assertThat(expectedResources.remove(), is(device));
        assertThat(expectedResources.remove(), is(device));
        assertThat(expectedResources.remove(), is(device));
    }

    @Test
    public void subscribeAddedAndUpdatedEventsForDeviceWithoutIdAndType() {
        Object subscriber = new Object() {
            @TradfriEventHandler({ EType.RESOURCE_ADDED, EType.RESOURCE_UPDATED })
            public void onAddedOrUpdated(TradfriEvent event, TradfriDevice device) {
                expectedEvents.add(event);
                expectedResources.add(device);
            }
        };
        this.resourceCache.subscribeEvents(subscriber);

        final TradfriCoapResourceProxy device = new TradfriCoapColorLightProxy(this.resourceCache, this.coapClient,
                this.scheduler);

        final TradfriCoapColorLight actualBulbData = createTradfriCoapColorLight();
        final String actualResourceId = actualBulbData.getInstanceId().get();
        assertNotNull(actualResourceId);

        // Generates event RESOURCE_ADDED
        device.initialize(actualBulbData);
        // Generates event RESOURCE_UPDATED
        this.resourceCache.updated(device);
        // Generates event RESOURCE_REMOVED
        this.resourceCache.remove(actualResourceId);

        assertThat(expectedEvents.size(), is(2));
        assertThat(expectedEvents.remove().getType(), is(EType.RESOURCE_ADDED));
        assertThat(expectedEvents.remove().getType(), is(EType.RESOURCE_UPDATED));

        assertThat(expectedResources.size(), is(2));
        assertThat(expectedResources.remove(), is(device));
        assertThat(expectedResources.remove(), is(device));
    }

}
