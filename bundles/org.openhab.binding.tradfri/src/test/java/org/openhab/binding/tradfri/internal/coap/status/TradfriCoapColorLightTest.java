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
public class TradfriCoapColorLightTest {

    private final Gson gson = new Gson();

    @Test
    public void testColorLightBulb() {
        String json = "{\"3\":{\"0\":\"IKEA of Sweden\",\"6\":1,\"1\":\"TRADFRI bulb E27 CWS opal 600lm\","
                + "\"2\":\"\",\"3\":\"1.3.013\"},\"9001\":\"LR Floor Lamp\",\"9003\":65539,\"9002\":1538425240,"
                + "\"9020\":1603481417,\"9054\":0,\"9019\":1,\"5750\":2,\"3311\":[{\"5850\":1,\"5851\":254,\"5707\":1490,"
                + "\"5708\":61206,\"5709\":40632,\"5710\":22282,\"5706\":\"da5d41\",\"9003\":0}]}";

        TradfriCoapColorLight bulb = this.gson.fromJson(json, TradfriCoapColorLight.class);

        // Check data of class TradfriCoapResource
        assertThat(bulb.getInstanceId(), is("65539"));
        assertThat(bulb.getName(), is("LR Floor Lamp"));
        assertThat(bulb.getTimestampCreatedAt(), is(1538425240L));

        // Check data of class TradfriCoapDevice
        assertThat(bulb.getDeviceType(), is(DeviceType.LIGHT));
        assertThat(bulb.getReachabilityState(), is(1));
        assertThat(bulb.getTimestampLastSeen(), is(1603481417L));
        assertThat(bulb.isAlive(), is(true));

        // Check data of class TradfriCoapDeviceInfo
        TradfriCoapDeviceInfo devInfo = bulb.getDeviceInfo();
        assertNotNull(devInfo);
        assertThat(devInfo.getVendor(), is("IKEA of Sweden"));
        assertThat(devInfo.getModel(), is("TRADFRI bulb E27 CWS opal 600lm"));
        assertThat(devInfo.getSerialNumber(), isEmptyString());
        assertThat(devInfo.getFirmware(), is("1.3.013"));
        assertThat(devInfo.getPowerSource(), is(1));
        assertThat(devInfo.getBatteryLevel(), is(-1)); // value is not available

        // Check data of class TradfriCoapColorLight
        TradfriCoapColorLightSetting bulbInfo = bulb.getLightSetting();
        assertNotNull(bulbInfo);
        // Check data of class TradfriCoapColorLightSetting
        assertThat(bulbInfo.getOnOff(), is(1));
        assertThat(bulbInfo.getDimmer(), is(254));
        assertThat(bulbInfo.getColor(), is("da5d41"));
        assertThat(bulbInfo.getHue(), is(1490));
        assertThat(bulbInfo.getSaturation(), is(61206));
        assertThat(bulbInfo.getColorX(), is(40632));
        assertThat(bulbInfo.getColorY(), is(22282));
    }
}
