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
package org.openhab.binding.tradfri.internal.coap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.ENDPOINT_DEVICES;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapLight;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapColorLightProxy;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapResourceProxy;
import org.openhab.binding.tradfri.internal.model.TradfriDevice;
import org.openhab.binding.tradfri.internal.model.TradfriEvent;
import org.openhab.binding.tradfri.internal.model.TradfriEvent.EType;
import org.openhab.binding.tradfri.internal.model.TradfriEventHandler;
import org.openhab.binding.tradfri.internal.model.TradfriLight;
import org.openhab.binding.tradfri.internal.model.TradfriResource;
import org.openhab.binding.tradfri.internal.model.TradfriThingResource;

import com.google.gson.Gson;

/**
 * Tests for {@link TradfriCoapResourceCache}.
 *
 * @author Jan Möller - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class TradfriCoapResourceCacheTest {

    private static final Gson gson = new Gson();

    @Mock
    private TradfriCoapClient coapClient;

    private TradfriCoapResourceCache resourceCache;

    private Queue<TradfriEvent> expectedEvents;
    private Queue<TradfriResource> expectedResources;

    private static String getCoapPayloadOfLightWithColorSupport() {
        return "{\"3\":{\"0\":\"IKEA of Sweden\",\"6\":1,\"1\":\"TRADFRI bulb E27 CWS opal 600lm\","
                + "\"2\":\"\",\"3\":\"1.3.013\"},\"9001\":\"LR Floor Lamp\",\"9003\":65539,\"9002\":1538425240,"
                + "\"9020\":1603481417,\"9054\":0,\"9019\":1,\"5750\":2,\"3311\":[{\"5850\":1,\"5851\":254,\"5707\":1490,"
                + "\"5708\":61206,\"5709\":40632,\"5710\":22282,\"5706\":\"da5d41\",\"9003\":0}]}";
    }

    @BeforeEach
    public void setUp() {
        this.resourceCache = new TradfriCoapResourceCache();
        this.expectedEvents = new LinkedList<TradfriEvent>();
        this.expectedResources = new LinkedList<TradfriResource>();
    }

    @AfterEach
    public void cleanUp() {
        this.resourceCache.clear();
        this.expectedEvents.clear();
        this.expectedResources.clear();
    }

    @Test
    @NonNullByDefault
    public void getLight() {
        final String coapPayload = getCoapPayloadOfLightWithColorSupport();
        final TradfriCoapLight bulbData = Objects.requireNonNull(gson.fromJson(coapPayload, TradfriCoapLight.class));

        final String coapPath = ENDPOINT_DEVICES + "/" + bulbData.getInstanceId().get();
        final TradfriCoapResourceProxy device = new TradfriCoapColorLightProxy(this.resourceCache, this.coapClient,
                coapPath, coapPayload);

        this.resourceCache.add(device);

        final String actualResourceId = bulbData.getInstanceId().get();
        assertNotNull(actualResourceId);

        final Optional<TradfriThingResource> asThingResource = this.resourceCache.getAs(actualResourceId,
                TradfriThingResource.class);
        assertThat(asThingResource.isPresent(), is(true));
        assertThat(asThingResource.get().getInstanceId().get(), is(actualResourceId));

        final Optional<TradfriDevice> asDevice = this.resourceCache.getAs(actualResourceId, TradfriDevice.class);
        assertThat(asDevice.isPresent(), is(true));
        assertThat(asDevice.get().getInstanceId().get(), is(actualResourceId));

        final Optional<TradfriLight> asDimmableLight = this.resourceCache.getAs(actualResourceId, TradfriLight.class);
        assertThat(asDimmableLight.isPresent(), is(true));
        assertThat(asDimmableLight.get().getInstanceId().get(), is(actualResourceId));
    }

    @Test
    public void subscribeAllEventsForDeviceWithoutIdAndType() {

        final String coapPayload = getCoapPayloadOfLightWithColorSupport();
        final TradfriCoapLight bulbData = Objects.requireNonNull(gson.fromJson(coapPayload, TradfriCoapLight.class));

        final String coapPath = ENDPOINT_DEVICES + "/" + bulbData.getInstanceId().get();
        final TradfriCoapResourceProxy device = new TradfriCoapColorLightProxy(this.resourceCache, this.coapClient,
                coapPath, coapPayload);

        final String actualResourceId = bulbData.getInstanceId().get();
        assertNotNull(actualResourceId);

        this.resourceCache.subscribeEvents((event) -> {
            expectedEvents.add(event);
            expectedResources.add(device);
        });

        // Generates event RESOURCE_ADDED
        this.resourceCache.add(device);
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
        final String coapPayload = getCoapPayloadOfLightWithColorSupport();
        final TradfriCoapLight bulbData = Objects.requireNonNull(gson.fromJson(coapPayload, TradfriCoapLight.class));

        final String coapPath = ENDPOINT_DEVICES + "/" + bulbData.getInstanceId().get();
        final TradfriCoapResourceProxy device = new TradfriCoapColorLightProxy(this.resourceCache, this.coapClient,
                coapPath, coapPayload);

        final String actualResourceId = bulbData.getInstanceId().get();
        assertNotNull(actualResourceId);

        this.resourceCache.subscribeEvents(EnumSet.of(EType.RESOURCE_ADDED, EType.RESOURCE_UPDATED), (event) -> {
            expectedEvents.add(event);
            expectedResources.add(device);
        });

        // Generates event RESOURCE_ADDED
        this.resourceCache.add(device);
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

    @Test
    public void unsubscribe() {
        final String coapPayload = getCoapPayloadOfLightWithColorSupport();
        final TradfriCoapLight bulbData = Objects.requireNonNull(gson.fromJson(coapPayload, TradfriCoapLight.class));

        final String actualResourceId = bulbData.getInstanceId().get();
        assertNotNull(actualResourceId);

        final String coapPath = ENDPOINT_DEVICES + "/" + actualResourceId;
        final TradfriCoapResourceProxy device = new TradfriCoapColorLightProxy(this.resourceCache, this.coapClient,
                coapPath, coapPayload);

        TradfriEventHandler subscriber1 = (event) -> {
            expectedEvents.add(event);
            expectedResources.add(device);
        };
        this.resourceCache.subscribeEvents(subscriber1);

        TradfriEventHandler subscriber2 = (event) -> {
            expectedEvents.add(event);
            expectedResources.add(device);
        };
        this.resourceCache.subscribeEvents(actualResourceId, EnumSet.of(EType.RESOURCE_ADDED, EType.RESOURCE_UPDATED),
                subscriber2);

        // Generates event RESOURCE_ADDED
        this.resourceCache.add(device);
        // Generates event RESOURCE_UPDATED
        this.resourceCache.updated(device);
        // Generates event RESOURCE_REMOVED
        this.resourceCache.remove(actualResourceId);

        assertThat(expectedEvents.size(), is(5));
        assertThat(expectedEvents.remove().getType(), is(EType.RESOURCE_ADDED));
        assertThat(expectedEvents.remove().getType(), is(EType.RESOURCE_ADDED));
        assertThat(expectedEvents.remove().getType(), is(EType.RESOURCE_UPDATED));
        assertThat(expectedEvents.remove().getType(), is(EType.RESOURCE_UPDATED));
        assertThat(expectedEvents.remove().getType(), is(EType.RESOURCE_REMOVED));

        assertThat(expectedResources.size(), is(5));
        assertThat(expectedResources.remove(), is(device));
        assertThat(expectedResources.remove(), is(device));
        assertThat(expectedResources.remove(), is(device));
        assertThat(expectedResources.remove(), is(device));
        assertThat(expectedResources.remove(), is(device));

        this.resourceCache.unsubscribeEvents(subscriber2);

        // Generates event RESOURCE_ADDED
        this.resourceCache.add(device);
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
}
