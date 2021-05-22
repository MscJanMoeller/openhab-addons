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

import java.util.function.Consumer;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParseException;

/**
 * The {@link TradfriCoapHandler} is used to handle the asynchronous CoAP responses.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Jan Möller - Refactored CoAP backend
 */
@NonNullByDefault
public class TradfriCoapHandler implements CoapHandler {

    private final Logger logger = LoggerFactory.getLogger(TradfriCoapHandler.class);

    private String path;
    private Consumer<String> consumer;

    /**
     * Constructor for using a future
     *
     * @param future the future to use for responses
     */
    public TradfriCoapHandler(String path, Consumer<String> consumer) {
        this.path = path;
        this.consumer = consumer;
    }

    @Override
    public void onLoad(@Nullable CoapResponse response) {
        if (response == null) {
            logger.trace("Received empty CoAP response");
            return;
        }

        logger.trace("Received CoAP response. Path: {}  Options: {}  Payload: {}", this.path, response.getOptions(),
                response.getResponseText());
        if (response.isSuccess()) {
            try {
                this.consumer.accept(response.getResponseText());
            } catch (JsonParseException ex) {
                logger.error("Coap response for path '{}' is no valid json: {}, {}", this.path,
                        response.getResponseText(), ex.getMessage());
            }
        } else {
            logger.warn("CoAP error: '{}' '{}'  Path: {}  Options: {}  Payload: {}", response.getCode(),
                    response.getCode().name(), this.path, response.getOptions(), response.getResponseText());
        }
    }

    @Override
    public void onError() {
        logger.warn("CoAP error. Failed to get data for path {}.", this.path);
    }
}
