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
package org.openhab.binding.tradfri.internal.coap.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapDevice.DeviceType;

import com.google.gson.Gson;

/**
 * Tests for {@link TradfriCoapDevice}.
 *
 * @author Jan Möller - Initial contribution
 */
public class TradfriCoapDeviceTest {

    private final Gson gson = new Gson();

    @Test
    public void testLightBulb() {
        String json = "{\"9003\":65553," + "\"9001\":\"Dining Table center\"," + "\"5750\":2," + "\"9019\":1,"
                + "\"9002\":1545594514," + "\"9020\":1590864848," + "\"9054\":0," + "\"3\":{\"0\":\"IKEA of Sweden\","
                + "\"1\":\"TRADFRI bulb E27 WS clear 950lm\"," + "\"2\":\"\"," + "\"3\":\"2.3.050\"," + "\"6\":1},"
                + "\"3311\":[{\"5850\":1," + "\"5851\":50," + "\"5717\":0,\"5711\":454," + "\"5709\":30138,"
                + "\"5710\":26909," + "\"5706\":\"f1e0b5\"," + "\"9003\":0}]}";

        final TradfriCoapDevice dev = Objects.requireNonNull(this.gson.fromJson(json, TradfriCoapDevice.class));

        // Check data of class TradfriCoapResource
        assertThat(dev.getInstanceId().get(), is("65553"));
        assertThat(dev.getName().get(), is("Dining Table center"));
        assertThat(dev.getTimestampCreatedAt(), is(1545594514L));

        // Check data of class TradfriCoapDevice
        assertThat(dev.getDeviceType(), is(DeviceType.LIGHT));
        assertThat(dev.getReachabilityState(), is(1));
        assertThat(dev.getTimestampLastSeen(), is(1590864848L));
        assertThat(dev.isAlive(), is(true));

        // Check data of class TradfriCoapDeviceInfo
        TradfriCoapDeviceInfo devInfo = dev.getDeviceInfo().get();
        assertNotNull(devInfo);
        assertThat(devInfo.getVendor().get(), is("IKEA of Sweden"));
        assertThat(devInfo.getModel().get(), is("TRADFRI bulb E27 WS clear 950lm"));
        assertThat(devInfo.getSerialNumber().get(), is(emptyString()));
        assertThat(devInfo.getFirmware().get(), is("2.3.050"));
        assertThat(devInfo.getPowerSource(), is(1));
        assertThat(devInfo.getBatteryLevel(), is(-1)); // value is not available
    }
}
