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
import static org.junit.Assert.assertThat;
import static org.mockito.AdditionalAnswers.answerVoid;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.ENDPOINT_DEVICES;

import java.net.URI;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Tests for {@link TradfriCoapTest}.
 *
 * @author Jan Möller - Initial contribution
 */
public class TradfriCoapClientTest {

    @Mock
    private CoapClient coapClient;
    @Mock
    private ScheduledExecutorService scheduler;

    private Queue<Request> expectedRequests;

    @Before
    public void setUp() {
        initMocks(this);
        this.expectedRequests = new LinkedList<Request>();
    }

    @After
    public void cleanUp() {
        this.expectedRequests.clear();
    }

    @Test
    public void get() {
        URI gatewayURI = URI.create("coaps://127.0.0.1:5684/");
        TradfriCoapClient client = new TradfriCoapClient(gatewayURI, coapClient, scheduler);

        // Stub behavior of CoapClient
        doAnswer(answerVoid((CoapHandler callback, Request request) -> expectedRequests.add(request))).when(coapClient)
                .advanced(any(CoapHandler.class), any(Request.class));

        client.get(ENDPOINT_DEVICES + "/65537", new CoapHandler() {
            @Override
            public void onLoad(@Nullable CoapResponse response) {
            }

            @Override
            public void onError() {
            }
        });

        assertThat(expectedRequests.size(), is(1));
        Request actualRequest = expectedRequests.remove();

        assertThat(actualRequest.getURI(), is("coaps://127.0.0.1/" + ENDPOINT_DEVICES + "/65537"));
        assertThat(actualRequest.getType(), is(Type.CON));
    }
}
