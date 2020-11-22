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

import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.tradfri.internal.model.TradfriResourceEventHandler;
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

    private static final HashMap<ThingTypeUID, @Nullable Class<? extends TradfriCoapDeviceProxy>> factoryMap = new HashMap<ThingTypeUID, @Nullable Class<? extends TradfriCoapDeviceProxy>>();

    private static final JsonParser parser = new JsonParser();

    private final String baseUri;
    private final Endpoint endpoint;

    private final ScheduledExecutorService scheduler;

    static {
        factoryMap.put(THING_TYPE_DIMMABLE_LIGHT, TradfriCoapDimmableLightProxy.class);
        factoryMap.put(THING_TYPE_COLOR_TEMP_LIGHT, TradfriCoapColorTempLightProxy.class);
        factoryMap.put(THING_TYPE_COLOR_LIGHT, TradfriCoapColorLightProxy.class);
    }

    public TradfriCoapProxyFactory(String baseUri, Endpoint endpoint, ScheduledExecutorService scheduler) {
        this.baseUri = baseUri;
        this.endpoint = endpoint;
        this.scheduler = scheduler;
    }

    public void createDeviceProxy(String id, TradfriResourceEventHandler callback) {
        TradfriCoapClient coapClient = new TradfriCoapClient(this.baseUri + "/" + ENDPOINT_DEVICES + "/" + id);
        createDeviceProxy(coapClient, callback);
    }

    public void createDeviceProxy(TradfriCoapClient coapClient, TradfriResourceEventHandler callback) {
        coapClient.get(new CoapHandler() {
            @Override
            public void onLoad(@Nullable CoapResponse response) {
                if (response == null) {
                    logger.trace("Received empty CoAP response");
                    return;
                }
                logger.trace("GatewayInfo CoAP response\noptions: {}\npayload: {}", response.getOptions(),
                        response.getResponseText());
                if (response.isSuccess()) {
                    try {
                        JsonObject devicePayload = parser.parse(response.getResponseText()).getAsJsonObject();

                        // Create proxy based on ThingTypeUID
                        ThingTypeUID thingType = TradfriThingTypeMap.getThingTypeFrom(devicePayload);
                        if (thingType != null) {
                            Class<? extends TradfriCoapDeviceProxy> proxyClass = factoryMap.get(thingType);
                            if (proxyClass != null) {
                                TradfriCoapResourceProxy newProxy = proxyClass
                                        .getConstructor(TradfriCoapClient.class, ScheduledExecutorService.class)
                                        .newInstance(coapClient, scheduler);
                                newProxy.onLoad(response);
                                callback.onUpdate(newProxy);
                            }
                        } else {
                            logger.info("Ignoring unknown device of TRADFRI gateway:\npayload: {}",
                                    response.getResponseText());
                        }
                    } catch (JsonSyntaxException ex) {
                        logger.error("Unexpected CoAP response: {}", response);
                    } catch (ReflectiveOperationException e) {
                        logger.error("Unexpected error while creating device proxy based on CoAP response: {}",
                                response);
                    } catch (IllegalArgumentException e) {
                        logger.error("Unexpected error while creating device proxy based on CoAP response: {}",
                                response);
                    }
                } else {
                    logger.error("CoAP error: {}. Failed to get device data for {}.", response.getCode(),
                            coapClient.getURI());
                }
            }

            @Override
            public void onError() {
                logger.warn("CoAP error. Failed to get device data for {}.", coapClient.getURI());
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
