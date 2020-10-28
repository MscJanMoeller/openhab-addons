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

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.ENDPOINT_DEVICES;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.model.TradfriResourceEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * {@link TradfriCoapProxyFactory} creates proxy objects for specific
 * single resources like a device, group or scene.
 *
 * @author Jan Möller - Initial contribution
 *
 */

@NonNullByDefault
public class TradfriCoapProxyFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Gson gson = new Gson();

    private final String baseUri;
    private final Endpoint endpoint;

    private final ScheduledExecutorService scheduler;

    public TradfriCoapProxyFactory(String baseUri, Endpoint endpoint, ScheduledExecutorService scheduler) {
        this.baseUri = baseUri;
        this.endpoint = endpoint;
        this.scheduler = scheduler;
    }

    public void createDeviceProxy(String id, TradfriResourceEventHandler callback) {
        TradfriCoapClient coapClient = new TradfriCoapClient(this.baseUri + "/" + ENDPOINT_DEVICES + "/" + id);
        coapClient.get(new CoapHandler() {
            @Override
            public void onLoad(@Nullable CoapResponse response) {
                if (response == null) {
                    logger.trace("Received empty GatewayInfo CoAP response");
                    return;
                }
                logger.trace("GatewayInfo CoAP response\noptions: {}\npayload: {}", response.getOptions(),
                        response.getResponseText());
                if (response.isSuccess()) {
                    try {
                        // TODO create proxy based on
                    } catch (JsonSyntaxException ex) {
                        logger.error("Unexpected data response: {}", response);
                    }
                } else {
                    logger.error("GatewayInfo CoAP error: {}", response.getCode());
                }
            }

            @Override
            public void onError() {
                logger.warn("CoAP error. Failed to get resource update for {}.", coapClient.getURI());
            }
        });
    }

    public TradfriCoapGroupProxy createGroupProxy(String id) {
        return new TradfriCoapGroupProxy(this.baseUri, id, this.endpoint, this.scheduler);
    }

    public TradfriCoapSceneProxy createSceneProxy(String id) {
        return new TradfriCoapSceneProxy(this.baseUri, id, this.endpoint, this.scheduler);
    }
}
