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
package org.openhab.binding.tradfri.internal.coap.status;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.openhab.binding.tradfri.internal.coap.status.TradfriDevice.DeviceType;

import com.google.gson.Gson;

/**
 * Tests for {@link TradfriDevice}.
 *
 * @author Jan Möller - Initial contribution
 */
public class TradfriDimmableLightTest {

    private final Gson gson = new Gson();

    @Test
    public void testDimmableLightBulb() {
        String json = "{\"3\":{\"0\":\"IKEA of Sweden\",\"6\":1,\"1\":\"TRADFRI Driver 10W\","
                + "\"2\":\"\",\"3\":\"1.2.245\"},\"9001\":\"BR Mirror Cabinet\",\"9003\":65565,"
                + "\"9002\":1600000754,\"9020\":1603996246,\"9054\":0,\"9019\":1,\"5750\":2,"
                + "\"3311\":[{\"5850\":1,\"5851\":254,\"9003\":0}]}";

        TradfriDimmableLight bulb = this.gson.fromJson(json, TradfriDimmableLight.class);

        // Check data of class TradfriResource
        assertThat(bulb.getInstanceId(), is("65565"));
        assertThat(bulb.getName(), is("BR Mirror Cabinet"));
        assertThat(bulb.getTimestampCreatedAt(), is(1600000754L));

        // Check data of class TradfriDevice
        assertThat(bulb.getDeviceType(), is(DeviceType.LIGHT));
        assertThat(bulb.getReachabilityState(), is(1));
        assertThat(bulb.getTimestampLastSeen(), is(1603996246L));
        assertThat(bulb.isAlive(), is(true));

        // Check data of class TradfriDeviceInfo
        TradfriDeviceInfo devInfo = bulb.getDeviceInfo();
        assertNotNull(devInfo);
        assertThat(devInfo.getVendor(), is("IKEA of Sweden"));
        assertThat(devInfo.getModel(), is("TRADFRI Driver 10W"));
        assertThat(devInfo.getSerialNumber(), isEmptyString());
        assertThat(devInfo.getFirmware(), is("1.2.245"));
        assertThat(devInfo.getPowerSource(), is(1));
        assertThat(devInfo.getBatteryLevel(), is(-1)); // value is not available

        // Check data of class TradfriDimmableLight
        TradfriDimmableLightSetting bulbInfo = bulb.getLightSetting();
        assertNotNull(bulbInfo);
        // Check data of class TradfriColorLightSetting
        assertThat(bulbInfo.getOnOff(), is(1));
        assertThat(bulbInfo.getDimmer(), is(254));
    }
}
