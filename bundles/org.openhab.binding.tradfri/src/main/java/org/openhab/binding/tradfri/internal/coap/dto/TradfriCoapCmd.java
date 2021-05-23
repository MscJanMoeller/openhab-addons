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

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapResourceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The {@link TradfriCoapCmd} class is used for a data transfer object (DTO). It represents the payload of CoAP commands
 * for devices and groups.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapCmd implements CoapHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static AtomicInteger numErrors = new AtomicInteger(0);

    private TradfriCoapResourceProxy proxy;
    private JsonObject payload;
    private JsonObject commandProperties;

    public TradfriCoapCmd(TradfriCoapResourceProxy proxy) {
        this(proxy, null);
    }

    public TradfriCoapCmd(TradfriCoapResourceProxy proxy, @Nullable String commandKey) {
        this.proxy = proxy;
        this.payload = new JsonObject();
        this.commandProperties = this.payload;
        if (commandKey != null) {
            this.commandProperties = new JsonObject();
            final JsonArray array = new JsonArray();
            array.add(this.commandProperties);
            this.payload.add(commandKey, array);
        }
    }

    public String getPayload() {
        return this.payload.toString();
    }

    @Override
    public void onLoad(@Nullable CoapResponse response) {
        if (response == null) {
            logger.trace("Received empty CoAP response.");
            return;
        }
        logger.trace("Processing command CoAP response. Options: {}  Payload: {}", response.getOptions(),
                response.getResponseText());
        if (!response.isSuccess()) {
            final int errors = numErrors.incrementAndGet();
            logger.error(
                    "CoAP error: '{}' '{}'. Failed to execute command for resource {}. Total num errors: {}  Command payload: {} ",
                    response.getCode(), response.getCode().name(), getInstanceId(), errors, this.payload);
        }
    }

    @Override
    public void onError() {
        final int errors = numErrors.incrementAndGet();
        logger.error(
                "CoAP error. Failed to execute command for resource {}.  Total num errors: {}  Command payload: {} ",
                getInstanceId(), errors, this.payload);
    }

    public static int getNumCommandErrors() {
        return numErrors.get();
    }

    protected String getInstanceId() {
        return this.proxy.getInstanceId().get();
    }

    protected TradfriCoapCmd addCommandProperty(String name, JsonElement value) {
        this.commandProperties.add(name, value);
        return this;
    }
}