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
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapGateway;

import com.google.gson.Gson;

/**
 * Tests for {@link TradfriCoapGateway}.
 *
 * @author Jan Möller - Initial contribution
 */
public class TradfriCoapGatwayDataTest {

    private final Gson gson = new Gson();

    @Test
    public void testGetter() {
        String json = "{" + "\"9093\":0," + "\"9092\":1," + "\"9061\":2," + "\"9077\":25," + "\"9078\":5,"
                + "\"9079\":6," + "\"9076\":10," + "\"9073\":29," + "\"9074\":7," + "\"9075\":59," + "\"9072\":3,"
                + "\"9080\":60," + "\"9071\":15," + "\"9200\":\"4ed67e38-526e-4411-a3c7-4b3eb38a3321\"," + "\"9055\":7,"
                + "\"9105\":16," + "\"9107\":17," + "\"9083\":\"N/A\"," + "\"9103\":\"\","
                + "\"9023\":\"0.tradfri.pool.ntp.org\"," + "\"9054\":18," + "\"9066\":19," + "\"9118\":20,"
                + "\"9059\":1590765905," + "\"9060\":\"2020-05-29T15:25:05.009464Z\"," + "\"9202\":1580328447,"
                + "\"9069\":1590082170," + "\"9029\":\"1.10.36\","
                /* Semantic of the following values is not known yet */
                + "\"9062\":0," + "\"9081\":\"7e24495204400179\"," + "\"9082\":false," + "\"9106\":0,"
                + "\"9201\":1\"}";

        TradfriCoapGateway gw = this.gson.fromJson(json, TradfriCoapGateway.class);

        assertThat(gw.getAlexaPairStatus(), is(0));
        assertThat(gw.getCertificateProvisioned(), is(1));
        assertThat(gw.getCommissioningMode(), is(2));
        assertThat(gw.getDstEndDay(), is(25));
        assertThat(gw.getDstEndHour(), is(5));
        assertThat(gw.getDstEndMinute(), is(6));
        assertThat(gw.getDstEndMonth(), is(10));
        assertThat(gw.getDstStartDay(), is(29));
        assertThat(gw.getDstStartHour(), is(7));
        assertThat(gw.getDstStartMinute(), is(59));
        assertThat(gw.getDstStartMonth(), is(3));
        assertThat(gw.getDstTimeOffSet(), is(60));
        assertThat(gw.getGatewayName(), is(nullValue())); /* not available */
        assertThat(gw.getGatewayTimeSource(), is(15));
        assertThat(gw.getGatewayUniqueId(), is("4ed67e38-526e-4411-a3c7-4b3eb38a3321"));
        assertThat(gw.getGatewayUpdateDetailsUrl(), is(nullValue())); /* not available */
        assertThat(gw.getGatewayUpdateProgress(), is(7));
        assertThat(gw.getGoogleHomePairStatus(), is(16));
        assertThat(gw.getHomeKitPairingStatus(), is(17));
        assertThat(gw.getHomekitSetupCode(), is("N/A"));
        assertThat(gw.getIotEndpoint(), isEmptyString());
        assertThat(gw.getNtpServerUrl(), is("0.tradfri.pool.ntp.org"));
        assertThat(gw.getOtaForceCheckUpdate(), is(nullValue())); /* not available */
        assertThat(gw.getOtaUpdateState(), is(18));
        assertThat(gw.getOtaUpdateType(), is(19));
        assertThat(gw.getSonosCertificateStatus(), is(20));
        assertThat(gw.getTimestampCurrentFormatUnix(), is(1590765905L));
        assertThat(gw.getTimestampCurrentFormatISO8601(), is("2020-05-29T15:25:05.009464Z"));
        assertThat(gw.getTimestampLastModification(), is(1580328447L));
        assertThat(gw.getTimestampUpdateAccepted(), is(1590082170L));
        assertThat(gw.getVersion(), is("1.10.36"));
    }
}
