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

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapDevice.DeviceType;

import com.google.gson.Gson;

/**
 * Tests for {@link TradfriCoapBlind}.
 *
 * @author Jan Möller - Initial contribution
 */
public class TradfriCoapBlindTest {

    private final Gson gson = new Gson();

    @Test
    public void testBlind() {
        String json = "{\"3\":{\"0\":\"IKEA of Sweden\",\"1\":\"FYRTUR block-out roller blind\",\"2\":\"\",\"3\":\"2.2.007\",\"6\":3,\"9\":77},\"5750\":7,\"9001\":\"Blind name\",\"9002\":1566141494,\"9003\":65601,\"9019\":1,\"9020\":1566402653,\"9054\":0,\"9084\":\" 9d 58 b0 2 4 6a df be 77 e5 c1 e0 a2 26 2e 57\",\"15015\":[{\"5536\":51.0,\"9003\":0}]}";

        final TradfriCoapBlind blind = Objects.requireNonNull(gson.fromJson(json, TradfriCoapBlind.class));

        // Check data of class TradfriCoapResource
        assertThat(blind.getInstanceId().get(), is("65601"));
        assertThat(blind.getName().get(), is("Blind name"));
        assertThat(blind.getTimestampCreatedAt(), is(1566141494L));

        // Check data of class TradfriCoapDevice
        assertThat(blind.getDeviceType(), is(DeviceType.BLIND));
        assertThat(blind.getReachabilityState(), is(1));
        assertThat(blind.getTimestampLastSeen(), is(1566402653L));
        assertThat(blind.isAlive(), is(true));

        // Check data of class TradfriCoapDeviceInfo
        final TradfriCoapDeviceInfo devInfo = blind.getDeviceInfo().get();
        assertThat(devInfo.getVendor().get(), is("IKEA of Sweden"));
        assertThat(devInfo.getModel().get(), is("FYRTUR block-out roller blind"));
        assertThat(devInfo.getSerialNumber().get(), is(emptyString()));
        assertThat(devInfo.getFirmware().get(), is("2.2.007"));
        assertThat(devInfo.getPowerSource(), is(3));
        assertThat(devInfo.getBatteryLevel(), is(77)); // value is not available

        // Check data of class TradfriCoapBlind
        final TradfriCoapBlindSetting blindInfo = blind.getBlindSetting().get();
        // Check data of class TradfriCoapBlindSetting
        assertThat(blindInfo.getPosition(), is(51F));
    }
}
