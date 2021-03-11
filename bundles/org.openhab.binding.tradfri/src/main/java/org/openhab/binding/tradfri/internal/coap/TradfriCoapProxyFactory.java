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

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.*;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.coap.proxy.TradfriCoapResourceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    private final JsonParser parser = new JsonParser();

    private final TradfriCoapResourceCache resourceCache;

    private final TradfriCoapClient coapClient;

    public TradfriCoapProxyFactory(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient) {
        this.resourceCache = resourceCache;
        this.coapClient = coapClient;
    }

    public void createAndAddDeviceProxy(String id) {
        createProxy(ENDPOINT_DEVICES + "/" + id);
    }

    public void createAndAddGroupProxy(String id) {
        createProxy(ENDPOINT_GROUPS + "/" + id);
    }

    public void createAndAddSceneProxy(String groupId, String sceneId) {
        createProxy(ENDPOINT_SCENES + "/" + groupId + "/" + sceneId);
    }

    private void createProxy(String relPath) {
        this.coapClient.get(relPath, new CoapHandler() {
            @Override
            public void onLoad(@Nullable CoapResponse response) {
                if (response == null) {
                    logger.trace("Received empty CoAP response");
                    return;
                }
                logger.trace("Creating proxy based on CoAP response. Options: {}  Payload: {}", response.getOptions(),
                        response.getResponseText());
                if (response.isSuccess()) {
                    try {
                        JsonObject payload = parser.parse(response.getResponseText()).getAsJsonObject();

                        // Create proxy based on coap payload
                        Class<? extends TradfriCoapResourceProxy> proxyClass = TradfriProxyClassMap
                                .getProxyClassFrom(payload);
                        if (proxyClass != null) {
                            resourceCache.add(proxyClass
                                    .getConstructor(TradfriCoapResourceCache.class, TradfriCoapClient.class,
                                            String.class, JsonObject.class)
                                    .newInstance(resourceCache, coapClient, relPath, payload)).initialize();
                        } else {
                            logger.info("Ignoring unknown device of TRADFRI gateway. Options: {}  Payload: {}",
                                    response.getOptions(), response.getResponseText());
                        }
                    } catch (JsonSyntaxException ex) {
                        logger.error("Unexpected CoAP response. Options: {}  Payload: {}", response.getOptions(),
                                response.getResponseText());
                    } catch (ReflectiveOperationException e) {
                        logger.error(
                                "Unexpected error while creating device proxy based on CoAP response. Options: {}  Payload: {}",
                                response.getOptions(), response.getResponseText());
                    } catch (IllegalArgumentException e) {
                        logger.error(
                                "Unexpected error while creating device proxy based on CoAP response. Options: {}  Payload: {}",
                                response.getOptions(), response.getResponseText());
                    }
                } else {
                    logger.error("CoAP error: {}. Failed to get device data for {}.", response.getCode(), relPath);
                }
            }

            @Override
            public void onError() {
                logger.warn("CoAP error. Failed to get device data for {}.", relPath);
            }
        });
    }
}
