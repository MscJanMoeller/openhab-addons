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
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.number.OrderingComparison.*;
import static org.junit.Assert.*;
import static org.mockito.AdditionalAnswers.answerVoid;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.ENDPOINT_DEVICES;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.binding.tradfri.internal.coap.command.TradfriCoapCommand;

/**
 * Tests for {@link TradfriCoapTest}.
 *
 * @author Jan Möller - Initial contribution
 */
public class TradfriCoapClientTest {

    @Mock
    private CoapClient coapClient;

    private ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(3);

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

        assertThat(actualRequest.getCode(), is(Code.GET));
        assertThat(actualRequest.getURI(), is("coaps://127.0.0.1/" + ENDPOINT_DEVICES + "/65537"));
        assertThat(actualRequest.getType(), is(Type.CON));
    }

    @Test
    public void observe() {
        URI gatewayURI = URI.create("coaps://127.0.0.1:5684/");
        TradfriCoapClient client = new TradfriCoapClient(gatewayURI, coapClient, scheduler);

        // Stub behavior of CoapClient
        when(coapClient.observe(any(Request.class), any(CoapHandler.class))).thenAnswer((invocation) -> {
            expectedRequests.add(invocation.getArgument(0));
            return mock(CoapObserveRelation.class);
        });

        CoapObserveRelation observeRelation = client.observe(ENDPOINT_DEVICES + "/65537", new CoapHandler() {
            @Override
            public void onLoad(@Nullable CoapResponse response) {
            }

            @Override
            public void onError() {
            }
        });

        assertNotNull(observeRelation);

        assertThat(expectedRequests.size(), is(1));
        Request actualRequest = expectedRequests.remove();

        assertThat(actualRequest.getCode(), is(Code.GET));
        assertThat(actualRequest.getURI(), is("coaps://127.0.0.1/" + ENDPOINT_DEVICES + "/65537"));
        assertThat(actualRequest.getType(), is(Type.CON));
        assertThat(actualRequest.isObserve(), is(true));
    }

    @Test
    public void execute() {
        URI gatewayURI = URI.create("coaps://127.0.0.1:5684/");
        TradfriCoapClient client = new TradfriCoapClient(gatewayURI, coapClient, scheduler);

        final int NUM_COMMANDS = 3;

        CountDownLatch commandsExecuted = new CountDownLatch(NUM_COMMANDS);

        ArrayList<Long> executionTimes = new ArrayList<Long>(NUM_COMMANDS + 1);

        // Stub behavior of CoapClient
        doAnswer(answerVoid((CoapHandler callback, Request request) -> {
            executionTimes.add(System.nanoTime());
            expectedRequests.add(request);
            commandsExecuted.countDown();
        })).when(coapClient).advanced(any(CoapHandler.class), any(Request.class));

        TradfriCoapCommand command = mock(TradfriCoapCommand.class);
        when(command.getPayload()).thenReturn("coap payload");

        executionTimes.add(System.nanoTime());

        client.execute(command, ENDPOINT_DEVICES + "/65537");
        client.execute(command, ENDPOINT_DEVICES + "/65538");
        client.execute(command, ENDPOINT_DEVICES + "/65539");

        try {
            commandsExecuted.await();
        } catch (InterruptedException e) {
        }

        assertThat(expectedRequests.size(), is(NUM_COMMANDS));

        final Request actualRequest1 = expectedRequests.remove();
        assertThat(actualRequest1.getCode(), is(Code.PUT));
        assertThat(actualRequest1.getURI(), is("coaps://127.0.0.1/" + ENDPOINT_DEVICES + "/65537"));
        assertThat(actualRequest1.getType(), is(Type.CON));
        assertThat(actualRequest1.getOptions().getContentFormat(), is(MediaTypeRegistry.TEXT_PLAIN));
        assertThat(actualRequest1.getPayloadString(), is("coap payload"));
        final long delay1 = TimeUnit.MILLISECONDS.convert(executionTimes.get(1) - executionTimes.get(0),
                TimeUnit.NANOSECONDS);
        assertThat(delay1, allOf(greaterThan(10L), lessThan(70L)));

        final Request actualRequest2 = expectedRequests.remove();
        assertThat(actualRequest2.getCode(), is(Code.PUT));
        assertThat(actualRequest2.getURI(), is("coaps://127.0.0.1/" + ENDPOINT_DEVICES + "/65538"));
        assertThat(actualRequest2.getType(), is(Type.CON));
        assertThat(actualRequest2.getOptions().getContentFormat(), is(MediaTypeRegistry.TEXT_PLAIN));
        assertThat(actualRequest2.getPayloadString(), is("coap payload"));
        final long delay2 = TimeUnit.MILLISECONDS.convert(executionTimes.get(2) - executionTimes.get(1),
                TimeUnit.NANOSECONDS);
        assertThat(delay2, allOf(greaterThan(40L), lessThan(90L)));

        final Request actualRequest3 = expectedRequests.remove();
        assertThat(actualRequest3.getCode(), is(Code.PUT));
        assertThat(actualRequest3.getURI(), is("coaps://127.0.0.1/" + ENDPOINT_DEVICES + "/65539"));
        assertThat(actualRequest3.getType(), is(Type.CON));
        assertThat(actualRequest3.getOptions().getContentFormat(), is(MediaTypeRegistry.TEXT_PLAIN));
        assertThat(actualRequest3.getPayloadString(), is("coap payload"));
        final long delay3 = TimeUnit.MILLISECONDS.convert(executionTimes.get(3) - executionTimes.get(2),
                TimeUnit.NANOSECONDS);
        assertThat(delay3, allOf(greaterThan(40L), lessThan(90L)));
    }
}
