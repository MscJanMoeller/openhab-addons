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
import org.openhab.binding.tradfri.internal.coap.status.TradfriCoapDevice.DeviceType;

import com.google.gson.Gson;

/**
 * Tests for {@link TradfriCoapDevice}.
 *
 * @author Jan Möller - Initial contribution
 */
public class TradfriCoapColorTempLightTest {

    private final Gson gson = new Gson();

    @Test
    public void testColorTempLightBulb() {
        String json = "{\"9003\":65553," + "\"9001\":\"Dining Table\"," + "\"5750\":2," + "\"9019\":1,"
                + "\"9002\":1545594514," + "\"9020\":1590864848," + "\"9054\":0," + "\"3\":{\"0\":\"IKEA of Sweden\","
                + "\"1\":\"TRADFRI bulb E27 WS clear 950lm\"," + "\"2\":\"\"," + "\"3\":\"2.3.050\"," + "\"6\":1},"
                + "\"3311\":[{\"5850\":1," + "\"5851\":50," + "\"5717\":0,\"5711\":454," + "\"5709\":30138,"
                + "\"5710\":26909," + "\"5706\":\"f1e0b5\"," + "\"9003\":0}]}";

        TradfriCoapColorTempLight bulb = this.gson.fromJson(json, TradfriCoapColorTempLight.class);

        // Check data of class TradfriCoapResource
        assertThat(bulb.getInstanceId(), is("65553"));
        assertThat(bulb.getName(), is("Dining Table"));
        assertThat(bulb.getTimestampCreatedAt(), is(1545594514L));

        // Check data of class TradfriCoapDevice
        assertThat(bulb.getDeviceType(), is(DeviceType.LIGHT));
        assertThat(bulb.getReachabilityState(), is(1));
        assertThat(bulb.getTimestampLastSeen(), is(1590864848L));
        assertThat(bulb.isAlive(), is(true));

        // Check data of class TradfriCoapDeviceInfo
        TradfriCoapDeviceInfo devInfo = bulb.getDeviceInfo();
        assertNotNull(devInfo);
        assertThat(devInfo.getVendor(), is("IKEA of Sweden"));
        assertThat(devInfo.getModel(), is("TRADFRI bulb E27 WS clear 950lm"));
        assertThat(devInfo.getSerialNumber(), isEmptyString());
        assertThat(devInfo.getFirmware(), is("2.3.050"));
        assertThat(devInfo.getPowerSource(), is(1));
        assertThat(devInfo.getBatteryLevel(), is(-1)); // value is not available

        // Check data of class TradfriCoapColorTempLight
        TradfriCoapColorTempLightSetting bulbInfo = bulb.getLightSetting();
        assertNotNull(bulbInfo);
        // Check data of class TradfriCoapColorTempLightSetting
        assertThat(bulbInfo.getOnOff(), is(1));
        assertThat(bulbInfo.getDimmer(), is(50));
        assertThat(bulbInfo.getColor(), is("f1e0b5"));
        assertThat(bulbInfo.getColorX(), is(30138));
        assertThat(bulbInfo.getColorY(), is(26909));
        assertThat(bulbInfo.getColorTemperature(), is(454));
    }
}
