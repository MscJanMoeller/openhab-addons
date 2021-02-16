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
package org.openhab.binding.tradfri.internal.coap.legacy;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * The {@link TradfriCoapHandler} is used to handle the asynchronous CoAP responses.
 * It can either be used with a callback class or with a future.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class TradfriCoapHandler implements CoapHandler {

    private final Logger logger = LoggerFactory.getLogger(TradfriCoapHandler.class);
    private final JsonParser parser = new JsonParser();

    private @Nullable CoapCallback callback;

    /**
     * Constructor for using a callback
     *
     * @param callback the callback to use for responses
     */
    public TradfriCoapHandler() {
    }

    /**
     * Constructor for using a callback
     *
     * @param callback the callback to use for responses
     */
    public TradfriCoapHandler(CoapCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onLoad(@Nullable CoapResponse response) {
        if (response == null) {
            logger.trace("received empty CoAP response");
            return;
        }
        logger.trace("Received CoAP response. Options: {}  Payload: {}", response.getOptions(),
                response.getResponseText());
        if (response.isSuccess()) {
            final CoapCallback callback = this.callback;
            if (callback != null) {
                try {
                    deliverPayload(this.parser.parse(response.getResponseText()));
                } catch (JsonParseException e) {
                    logger.warn("Observed value is no valid json: {}, {}", response.getResponseText(), e.getMessage());
                }
            }
        } else {
            logger.debug("CoAP error: '{}' '{}'   payload: '{}'", response.getCode(), response.getCode().name(),
                    response.getResponseText());
            if (callback != null) {
                callback.onError(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            }
        }
    }

    @Override
    public void onError() {
        logger.debug("CoAP onError");
        if (this.callback != null) {
            this.callback.onError(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
    }

    /**
     * This is being called, if new data is received from a CoAP request.
     *
     * @param data the received json structure
     */
    protected void deliverPayload(JsonElement data) {
        if (this.callback != null) {
            this.callback.onUpdate(data);
        }
    }
}
