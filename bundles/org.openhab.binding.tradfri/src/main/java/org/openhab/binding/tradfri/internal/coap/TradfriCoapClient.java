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

import java.net.URI;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapCmd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TradfriCoapClient} encapsulates access to the
 * plain {@link CoapClient} from californium.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Jan Möller - Refactoring
 */
@NonNullByDefault
public class TradfriCoapClient {

    private static final long TIMEOUT = 2000;
    private static final long COMMAND_DELAY_MILLIS = 70;

    private final Logger logger = LoggerFactory.getLogger(TradfriCoapClient.class);

    private final CoapClient coapClient;

    private final URI gatewayURI;
    private final ScheduledExecutorService scheduler;

    private final Deque<ScheduledFuture<?>> commandsQueue = new ConcurrentLinkedDeque<>();

    public TradfriCoapClient(URI gatewayURI, CoapClient coapClient, ScheduledExecutorService scheduler) {
        this.gatewayURI = gatewayURI;
        this.scheduler = scheduler;
        this.coapClient = coapClient;
        this.coapClient.setTimeout(TIMEOUT);
    }

    public URI getGatewayURI() {
        return this.gatewayURI;
    }

    public boolean ping() {
        return this.coapClient.ping();
    }

    public void get(String relPath, CoapHandler handler) {
        this.coapClient.advanced(handler, newGet(relPath));
    }

    public CoapObserveRelation observe(String relPath, CoapHandler handler) {
        return this.coapClient.observe(newGet(relPath).setObserve(), handler);
    }

    public ScheduledFuture<?> poll(String relPath, CoapHandler handler, long pollPeriod) {
        return this.scheduler.scheduleWithFixedDelay(() -> get(relPath, handler), 1, pollPeriod, TimeUnit.SECONDS);
    }

    public synchronized void execute(TradfriCoapCmd command, String relPath) {
        final long delay = getCurrentCommandDelay();

        this.commandsQueue.add(this.scheduler.schedule(() -> {
            if (logger.isTraceEnabled()) {
                logger.trace("CoAP PUT request. URI: {} Payload: {}", getResourceURI(relPath).toString(),
                        command.getPayload());
            }
            this.coapClient.advanced(command, newPut(relPath, command.getPayload()));
        }, delay, TimeUnit.MILLISECONDS));

        this.commandsQueue.add(this.scheduler.schedule(() -> {
            while (!this.commandsQueue.isEmpty() && commandsQueue.peek().isDone()) {
                this.commandsQueue.poll();
            }
        }, delay + COMMAND_DELAY_MILLIS, TimeUnit.MILLISECONDS));
    }

    public void dispose() {
        this.coapClient.shutdown();

        // TODO cleanup pending commands
    }

    private long getCurrentCommandDelay() {
        return this.commandsQueue.isEmpty() ? 0
                : Math.max(0, this.commandsQueue.peekLast().getDelay(TimeUnit.MILLISECONDS));
    }

    private Request newGet(String path) {
        return applyRequestType(Request.newGet().setURI(getResourceURI(path)));
    }

    private Request newPut(String path, String payload) {
        final Request request = applyRequestType(Request.newPut().setURI(getResourceURI(path)));
        request.setPayload(payload);
        request.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
        return request;
    }

    private Request applyRequestType(Request request) {
        request.setType(Type.CON);
        return request;
    }

    private URI getResourceURI(String path) {
        return getGatewayURI().resolve(path);
    }
}
