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

package org.openhab.binding.tradfri.internal.coap.proxy;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapClient;
import org.openhab.binding.tradfri.internal.coap.TradfriCoapResourceCache;
import org.openhab.binding.tradfri.internal.coap.dto.TradfriCoapScene;
import org.openhab.binding.tradfri.internal.model.TradfriScene;

/**
 * {@link TradfriCoapSceneProxy} observes changes of a single scene
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapSceneProxy extends TradfriCoapResourceProxy implements TradfriScene {

    private final String groupID;

    public TradfriCoapSceneProxy(TradfriCoapResourceCache resourceCache, TradfriCoapClient coapClient, String coapPath,
            String coapPayload) {
        super(resourceCache, coapClient, coapPath, dtoFrom(coapPayload, TradfriCoapScene.class));

        this.groupID = coapPath.split("/")[1];
    }

    @Override
    public String getGroupID() {
        return this.groupID;
    }

    @Override
    public Optional<String> getSceneName() {
        return getDataAs(TradfriCoapScene.class).flatMap(scene -> scene.getName());
    }

    @Override
    protected TradfriCoapScene parsePayload(String coapPayload) {
        return dtoFrom(coapPayload, TradfriCoapScene.class);
    }
}
