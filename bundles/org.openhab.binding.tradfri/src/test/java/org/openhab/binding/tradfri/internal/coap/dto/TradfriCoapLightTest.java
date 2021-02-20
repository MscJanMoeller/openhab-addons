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
package org.openhab.binding.tradfri.internal.coap.dto;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapDeviceInfo;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapLight;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapLightSetting;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapDevice.DeviceType;

import com.google.gson.Gson;

/**
 * Tests for {@link TradfriCoapLight}.
 *
 * @author Jan Möller - Initial contribution
 */
public class TradfriCoapLightTest {

    private final Gson gson = new Gson();

    @Test
    public void testDimmableLightBulb() {
        String json = "{\"3\":{\"0\":\"IKEA of Sweden\",\"6\":1,\"1\":\"TRADFRI Driver 10W\","
                + "\"2\":\"\",\"3\":\"1.2.245\"},\"9001\":\"BR Mirror Cabinet\",\"9003\":65565,"
                + "\"9002\":1600000754,\"9020\":1603996246,\"9054\":0,\"9019\":1,\"5750\":2,"
                + "\"3311\":[{\"5850\":1,\"5851\":254,\"9003\":0}]}";

        TradfriCoapLight bulb = this.gson.fromJson(json, TradfriCoapLight.class);

        // Check data of class TradfriCoapResource
        assertThat(bulb.getInstanceId().get(), is("65565"));
        assertThat(bulb.getName().get(), is("BR Mirror Cabinet"));
        assertThat(bulb.getTimestampCreatedAt(), is(1600000754L));

        // Check data of class TradfriCoapDevice
        assertThat(bulb.getDeviceType(), is(DeviceType.LIGHT));
        assertThat(bulb.getReachabilityState(), is(1));
        assertThat(bulb.getTimestampLastSeen(), is(1603996246L));
        assertThat(bulb.isAlive(), is(true));

        // Check data of class TradfriCoapDeviceInfo
        TradfriCoapDeviceInfo devInfo = bulb.getDeviceInfo().get();
        assertNotNull(devInfo);
        assertThat(devInfo.getVendor().get(), is("IKEA of Sweden"));
        assertThat(devInfo.getModel().get(), is("TRADFRI Driver 10W"));
        assertThat(devInfo.getSerialNumber().get(), isEmptyString());
        assertThat(devInfo.getFirmware().get(), is("1.2.245"));
        assertThat(devInfo.getPowerSource(), is(1));
        assertThat(devInfo.getBatteryLevel(), is(-1)); // value is not available

        // Check data of class TradfriCoapLight
        TradfriCoapLightSetting bulbInfo = bulb.getLightSetting().get();
        assertNotNull(bulbInfo);
        // Check data of class TradfriCoapLightSetting
        assertThat(bulbInfo.getOnOff(), is(1));
        assertThat(bulbInfo.getDimmer(), is(254));
        assertThat(bulbInfo.getColorTemperature(), is(-1));
        assertThat(bulbInfo.getColorX(), is(-1));
        assertThat(bulbInfo.getColorY(), is(-1));
        assertThat(bulbInfo.getColor(), is(nullValue()));
        assertThat(bulbInfo.getHue(), is(-1));
        assertThat(bulbInfo.getSaturation(), is(-1));
    }

    @Test
    public void testColorTempLightBulb() {
        String json = "{\"9003\":65553," + "\"9001\":\"Dining Table\"," + "\"5750\":2," + "\"9019\":1,"
                + "\"9002\":1545594514," + "\"9020\":1590864848," + "\"9054\":0," + "\"3\":{\"0\":\"IKEA of Sweden\","
                + "\"1\":\"TRADFRI bulb E27 WS clear 950lm\"," + "\"2\":\"\"," + "\"3\":\"2.3.050\"," + "\"6\":1},"
                + "\"3311\":[{\"5850\":1," + "\"5851\":50," + "\"5717\":0,\"5711\":454," + "\"5709\":30138,"
                + "\"5710\":26909," + "\"5706\":\"f1e0b5\"," + "\"9003\":0}]}";

        TradfriCoapLight bulb = this.gson.fromJson(json, TradfriCoapLight.class);

        // Check data of class TradfriCoapResource
        assertThat(bulb.getInstanceId().get(), is("65553"));
        assertThat(bulb.getName().get(), is("Dining Table"));
        assertThat(bulb.getTimestampCreatedAt(), is(1545594514L));

        // Check data of class TradfriCoapDevice
        assertThat(bulb.getDeviceType(), is(DeviceType.LIGHT));
        assertThat(bulb.getReachabilityState(), is(1));
        assertThat(bulb.getTimestampLastSeen(), is(1590864848L));
        assertThat(bulb.isAlive(), is(true));

        // Check data of class TradfriCoapDeviceInfo
        TradfriCoapDeviceInfo devInfo = bulb.getDeviceInfo().get();
        assertNotNull(devInfo);
        assertThat(devInfo.getVendor().get(), is("IKEA of Sweden"));
        assertThat(devInfo.getModel().get(), is("TRADFRI bulb E27 WS clear 950lm"));
        assertThat(devInfo.getSerialNumber().get(), isEmptyString());
        assertThat(devInfo.getFirmware().get(), is("2.3.050"));
        assertThat(devInfo.getPowerSource(), is(1));
        assertThat(devInfo.getBatteryLevel(), is(-1)); // value is not available

        // Check data of class TradfriCoapLight
        TradfriCoapLightSetting bulbInfo = bulb.getLightSetting().get();
        assertNotNull(bulbInfo);
        // Check data of class TradfriCoapLightSetting
        assertThat(bulbInfo.getOnOff(), is(1));
        assertThat(bulbInfo.getDimmer(), is(50));
        assertThat(bulbInfo.getColorTemperature(), is(454));
        assertThat(bulbInfo.getColorX(), is(30138));
        assertThat(bulbInfo.getColorY(), is(26909));
        assertThat(bulbInfo.getColor(), is("f1e0b5"));
        assertThat(bulbInfo.getHue(), is(-1));
        assertThat(bulbInfo.getSaturation(), is(-1));
    }

    @Test
    public void testColorLightBulb() {
        String json = "{\"3\":{\"0\":\"IKEA of Sweden\",\"6\":1,\"1\":\"TRADFRI bulb E27 CWS opal 600lm\","
                + "\"2\":\"\",\"3\":\"1.3.013\"},\"9001\":\"LR Floor Lamp\",\"9003\":65539,\"9002\":1538425240,"
                + "\"9020\":1603481417,\"9054\":0,\"9019\":1,\"5750\":2,\"3311\":[{\"5850\":1,\"5851\":254,\"5707\":1490,"
                + "\"5708\":61206,\"5709\":40632,\"5710\":22282,\"5706\":\"da5d41\",\"9003\":0}]}";

        TradfriCoapLight bulb = this.gson.fromJson(json, TradfriCoapLight.class);

        // Check data of class TradfriCoapResource
        assertThat(bulb.getInstanceId().get(), is("65539"));
        assertThat(bulb.getName().get(), is("LR Floor Lamp"));
        assertThat(bulb.getTimestampCreatedAt(), is(1538425240L));

        // Check data of class TradfriCoapDevice
        assertThat(bulb.getDeviceType(), is(DeviceType.LIGHT));
        assertThat(bulb.getReachabilityState(), is(1));
        assertThat(bulb.getTimestampLastSeen(), is(1603481417L));
        assertThat(bulb.isAlive(), is(true));

        // Check data of class TradfriCoapDeviceInfo
        TradfriCoapDeviceInfo devInfo = bulb.getDeviceInfo().get();
        assertNotNull(devInfo);
        assertThat(devInfo.getVendor().get(), is("IKEA of Sweden"));
        assertThat(devInfo.getModel().get(), is("TRADFRI bulb E27 CWS opal 600lm"));
        assertThat(devInfo.getSerialNumber().get(), isEmptyString());
        assertThat(devInfo.getFirmware().get(), is("1.3.013"));
        assertThat(devInfo.getPowerSource(), is(1));
        assertThat(devInfo.getBatteryLevel(), is(-1)); // value is not available

        // Check data of class TradfriCoapLight
        TradfriCoapLightSetting bulbInfo = bulb.getLightSetting().get();
        assertNotNull(bulbInfo);
        // Check data of class TradfriCoapLightSetting
        assertThat(bulbInfo.getOnOff(), is(1));
        assertThat(bulbInfo.getDimmer(), is(254));
        assertThat(bulbInfo.getColorTemperature(), is(-1));
        assertThat(bulbInfo.getColorX(), is(40632));
        assertThat(bulbInfo.getColorY(), is(22282));
        assertThat(bulbInfo.getColor(), is("da5d41"));
        assertThat(bulbInfo.getHue(), is(1490));
        assertThat(bulbInfo.getSaturation(), is(61206));
    }
}
