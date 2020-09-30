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
package org.openhab.binding.tradfri.internal.model;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.openhab.binding.tradfri.internal.coap.status.TradfriDevice;
import org.openhab.binding.tradfri.internal.coap.status.TradfriDeviceInfo;
import org.openhab.binding.tradfri.internal.coap.status.TradfriDevice.DeviceType;

import com.google.gson.Gson;

/**
 * Tests for {@link TradfriDevice}.
 *
 * @author Jan Möller - Initial contribution
 */
public class TradfriDeviceTest {

    private final Gson gson = new Gson();

    @Test
    public void testLightBulb() {
        String json = "{\"9003\":65553," + "\"9001\":\"Dining Table center\"," + "\"5750\":2," + "\"9019\":1,"
                + "\"9002\":1545594514," + "\"9020\":1590864848," + "\"9054\":0," + "\"3\":{\"0\":\"IKEA of Sweden\","
                + "\"1\":\"TRADFRI bulb E27 WS clear 950lm\"," + "\"2\":\"\"," + "\"3\":\"2.3.050\"," + "\"6\":1},"
                + "\"3311\":[{\"5850\":1," + "\"5851\":50," + "\"5717\":0,\"5711\":454," + "\"5709\":30138,"
                + "\"5710\":26909," + "\"5706\":\"f1e0b5\"," + "\"9003\":0}]}";

        TradfriDevice dev = this.gson.fromJson(json, TradfriDevice.class);

        // Check data of class TradfriResource
        assertThat(dev.getInstanceId(), is("65553"));
        assertThat(dev.getName(), is("Dining Table center"));
        assertThat(dev.getTimestampCreatedAt(), is(1545594514L));

        // Check data of class TradfriDevice
        assertThat(dev.getDeviceType(), is(DeviceType.LIGHT));
        assertThat(dev.getReachabilityState(), is(1));
        assertThat(dev.getTimestampLastSeen(), is(1590864848L));
        assertThat(dev.isAlive(), is(true));

        // Check data of class TradfriDeviceInfo
        TradfriDeviceInfo devInfo = dev.getDeviceInfo();
        assertThat(devInfo.getVendor(), is("IKEA of Sweden"));
        assertThat(devInfo.getModel(), is("TRADFRI bulb E27 WS clear 950lm"));
        assertThat(devInfo.getSerialNumber(), isEmptyString());
        assertThat(devInfo.getFirmware(), is("2.3.050"));
        assertThat(devInfo.getPowerSource(), is(1));
        assertThat(devInfo.getBatteryLevel(), is(-1)); // value is not available
    }
}
