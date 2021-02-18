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

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapResourceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private String payload;

    public TradfriCoapCmd(TradfriCoapResourceProxy proxy, JsonObject payload) {
        this.proxy = proxy;
        this.payload = payload.toString();
    }

    public String getPayload() {
        return this.payload;
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
                    response.getCode(), response.getCode().name(), this.proxy.getInstanceId().get(), errors,
                    this.payload);
        }
    }

    @Override
    public void onError() {
        final int errors = numErrors.incrementAndGet();
        logger.error(
                "CoAP error. Failed to execute command for resource {}.  Total num errors: {}  Command payload: {} ",
                this.proxy.getInstanceId().get(), errors, this.payload);
    }

    public static int getNumCommandErrors() {
        return numErrors.get();
    }
}
