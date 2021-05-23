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
package org.openhab.binding.tradfri.internal.coap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.AdditionalAnswers.answerVoid;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.ENDPOINT_DEVICES;

import java.net.URI;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapCmd;

/**
 * Tests for {@link TradfriCoapClient}.
 *
 * @author Jan Möller - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class TradfriCoapClientTest {

    @Mock
    private CoapClient coapClient;

    @Mock
    private ScheduledExecutorService scheduler;

    private Queue<Request> expectedRequests;

    @BeforeEach
    public void setUp() {
        this.expectedRequests = new LinkedList<Request>();
    }

    @AfterEach
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

        client.get(ENDPOINT_DEVICES + "/65537", (String) -> {
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

        CoapObserveRelation observeRelation = client.observe(ENDPOINT_DEVICES + "/65537", (String) -> {
        });

        assertNotNull(observeRelation);

        assertThat(expectedRequests.size(), is(1));
        final Request actualRequest = expectedRequests.remove();
        assertThat(actualRequest.getCode(), is(Code.GET));
        assertThat(actualRequest.getURI(), is("coaps://127.0.0.1/" + ENDPOINT_DEVICES + "/65537"));
        assertThat(actualRequest.getType(), is(Type.CON));
        assertThat(actualRequest.isObserve(), is(true));
    }

    @Test
    public void execute() {
        URI gatewayURI = URI.create("coaps://127.0.0.1:5684/");
        TradfriCoapClient client = new TradfriCoapClient(gatewayURI, coapClient, scheduler);

        final Queue<Runnable> scheduledJobs = new LinkedList<Runnable>();
        final Queue<Long> expectedDelays = new LinkedList<Long>();

        final ScheduledFuture<?> job = mock(ScheduledFuture.class);
        // Stub behavior of scheduler
        when(scheduler.schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS))).thenAnswer((invocation) -> {
            scheduledJobs.add(invocation.getArgument(0));
            expectedDelays.add(invocation.getArgument(1));
            return job;
        });

        // Stub behavior of CoapClient
        doAnswer(answerVoid((CoapHandler callback, Request request) -> {
            expectedRequests.add(request);
        })).when(coapClient).advanced(any(CoapHandler.class), any(Request.class));

        // Stub behavior of TradfriCoapCmd
        TradfriCoapCmd command = mock(TradfriCoapCmd.class);
        when(command.getPayload()).thenReturn("coap payload");

        // Execute first CoAP command at time 0 ms
        client.execute(command, ENDPOINT_DEVICES + "/65537");
        // Verify that 2 jobs are scheduled
        assertThat(scheduledJobs.size(), is(2));
        // Verify delays of scheduled jobs
        assertThat(expectedDelays.size(), is(2));
        assertThat(expectedDelays.poll(), is(0L));
        assertThat(expectedDelays.poll(), is(70L));

        // Simulate behavior of scheduler - execute job1 at time 1 ms
        Objects.requireNonNull(scheduledJobs.poll()).run();
        // Verify result of executed job1
        assertThat(expectedRequests.size(), is(1));
        final Request actualRequest1 = Objects.requireNonNull(expectedRequests.poll());
        assertThat(actualRequest1.getCode(), is(Code.PUT));
        assertThat(actualRequest1.getURI(), is("coaps://127.0.0.1/" + ENDPOINT_DEVICES + "/65537"));
        assertThat(actualRequest1.getType(), is(Type.CON));
        assertThat(actualRequest1.getOptions().getContentFormat(), is(MediaTypeRegistry.TEXT_PLAIN));
        assertThat(actualRequest1.getPayloadString(), is("coap payload"));

        // Execute second CoAP command at time 5 ms
        // Simulate that there are already commands in the queue
        when(job.getDelay(TimeUnit.MILLISECONDS)).thenReturn(65L);
        client.execute(command, ENDPOINT_DEVICES + "/65538");
        // Verify that in total 3 jobs are pending
        assertThat(scheduledJobs.size(), is(3));
        // Verify delays of scheduled jobs
        assertThat(expectedDelays.size(), is(2));
        assertThat(expectedDelays.poll(), is(65L));
        assertThat(expectedDelays.poll(), is(135L));

        // Simulate behavior of scheduler - execute job2 at time 70 ms
        // job1 already executed, job2 and further jobs not
        when(job.isDone()).thenReturn(true, false);
        Objects.requireNonNull(scheduledJobs.poll()).run();

        // Simulate behavior of scheduler - execute job3 at time 71 ms
        Objects.requireNonNull(scheduledJobs.poll()).run();
        // Verify result of executed job3
        assertThat(expectedRequests.size(), is(1));
        final Request actualRequest2 = Objects.requireNonNull(expectedRequests.poll());
        assertThat(actualRequest2.getCode(), is(Code.PUT));
        assertThat(actualRequest2.getURI(), is("coaps://127.0.0.1/" + ENDPOINT_DEVICES + "/65538"));
        assertThat(actualRequest2.getType(), is(Type.CON));
        assertThat(actualRequest2.getOptions().getContentFormat(), is(MediaTypeRegistry.TEXT_PLAIN));
        assertThat(actualRequest2.getPayloadString(), is("coap payload"));

        // Execute third CoAP command at time 100 ms
        // Simulate that there are already commands in the queue
        when(job.getDelay(TimeUnit.MILLISECONDS)).thenReturn(40L);
        client.execute(command, ENDPOINT_DEVICES + "/65539");
        // Verify that in total 3 jobs are pending
        assertThat(scheduledJobs.size(), is(3));
        // Verify delays of scheduled jobs
        assertThat(expectedDelays.size(), is(2));
        assertThat(expectedDelays.poll(), is(40L));
        assertThat(expectedDelays.poll(), is(110L));

        // Simulate behavior of scheduler - execute job4 at time 140 ms
        // job2 & job3 already executed, job4 and further jobs not
        when(job.isDone()).thenReturn(true, true, false);
        Objects.requireNonNull(scheduledJobs.poll()).run();

        // Simulate behavior of scheduler - execute job5 at time 141 ms
        Objects.requireNonNull(scheduledJobs.poll()).run();
        // Verify result of executed job5
        assertThat(expectedRequests.size(), is(1));
        final Request actualRequest3 = Objects.requireNonNull(expectedRequests.poll());
        assertThat(actualRequest3.getCode(), is(Code.PUT));
        assertThat(actualRequest3.getURI(), is("coaps://127.0.0.1/" + ENDPOINT_DEVICES + "/65539"));
        assertThat(actualRequest3.getType(), is(Type.CON));
        assertThat(actualRequest3.getOptions().getContentFormat(), is(MediaTypeRegistry.TEXT_PLAIN));
        assertThat(actualRequest3.getPayloadString(), is("coap payload"));

        // Simulate behavior of scheduler - execute job6 at time 210 ms
        // job4 & job5 already executed, job6 not
        when(job.isDone()).thenReturn(true, true, false);
        Objects.requireNonNull(scheduledJobs.poll()).run();

        // Execute fourth CoAP command at time 220 ms
        // Simulate that there is a completed job in the queue
        when(job.getDelay(TimeUnit.MILLISECONDS)).thenReturn(-10L);
        client.execute(command, ENDPOINT_DEVICES + "/65540");
        // Verify that in total 2 jobs are pending
        assertThat(scheduledJobs.size(), is(2));
        // Verify delays of scheduled jobs
        assertThat(expectedDelays.size(), is(2));
        assertThat(expectedDelays.poll(), is(0L));
        assertThat(expectedDelays.poll(), is(70L));

        // Simulate behavior of scheduler - execute job7 at time 221 ms
        Objects.requireNonNull(scheduledJobs.poll()).run();
        // Verify result of executed job7
        assertThat(expectedRequests.size(), is(1));
        final Request actualRequest4 = Objects.requireNonNull(expectedRequests.poll());
        assertThat(actualRequest4.getCode(), is(Code.PUT));
        assertThat(actualRequest4.getURI(), is("coaps://127.0.0.1/" + ENDPOINT_DEVICES + "/65540"));
        assertThat(actualRequest4.getType(), is(Type.CON));
        assertThat(actualRequest4.getOptions().getContentFormat(), is(MediaTypeRegistry.TEXT_PLAIN));
        assertThat(actualRequest4.getPayloadString(), is("coap payload"));

        // Simulate behavior of scheduler - execute job8 at time 280 ms
        // job6 & job7 already executed, job8 not
        when(job.isDone()).thenReturn(true, true, false);
        Objects.requireNonNull(scheduledJobs.poll()).run();
    }
}
