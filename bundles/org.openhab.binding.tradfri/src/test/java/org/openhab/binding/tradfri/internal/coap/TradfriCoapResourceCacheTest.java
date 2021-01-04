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
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapColorLight;
import org.openhab.binding.tradfri.internal.model.TradfriDevice;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriResource;

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

    private List<TradfriEvent> receivedEvents;
    private List<TradfriResource> receivedResources;

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
        this.receivedEvents = new LinkedList<TradfriEvent>();
        this.receivedResources = new LinkedList<TradfriResource>();
    }

    @After
    public void cleanUp() {
        this.resourceCache.clear();
        this.receivedEvents.clear();
        this.receivedResources.clear();
    }

    @Test
    public void subscribeEventsForDeviceWithoutIdAndType() {
        Object subscriber = new Object() {
            @TradfriEventHandler
            public void onAddedOrUpdatedOrRemoved(TradfriEvent event, TradfriDevice device) {
                receivedEvents.add(event);
                receivedResources.add(device);
            }
        };

        this.resourceCache.subscribeEvent(subscriber);

        final TradfriCoapResourceProxy device = new TradfriCoapColorLightProxy(this.resourceCache, this.coapClient,
                this.scheduler);

        final TradfriCoapColorLight actualBulbData = createTradfriCoapColorLight();
        final String actualResourceId = actualBulbData.getInstanceId();
        assertNotNull(actualResourceId);

        // Generates event RESOURCE_ADDED
        device.initialize(actualBulbData);
        // Generates event RESOURCE_UPDATED
        this.resourceCache.updated(device);
        // Generates event RESOURCE_REMOVED
        this.resourceCache.remove(actualResourceId);

        assertThat(receivedEvents.size(), is(3));
    }

}
