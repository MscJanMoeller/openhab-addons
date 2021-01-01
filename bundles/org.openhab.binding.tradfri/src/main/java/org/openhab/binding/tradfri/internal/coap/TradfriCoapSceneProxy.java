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

import static org.openhab.binding.tradfri.internal.TradfriBindingConstants.ENDPOINT_SCENES;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.coap.status.TradfriScene;
import org.openhab.binding.tradfri.internal.model.TradfriSceneProxy;

/**
 * {@link TradfriCoapSceneProxy} observes changes of a single scene
 *
 * @author Jan Möller - Initial contribution
 *
 */
@NonNullByDefault
public class TradfriCoapSceneProxy extends TradfriCoapResourceProxy implements TradfriSceneProxy {

    public TradfriCoapSceneProxy(TradfriCoapResourceStorage resourceStorage, String gatewayUri, String groupId,
            Endpoint endpoint, ScheduledExecutorService scheduler) {
        super(resourceStorage, gatewayUri + "/" + ENDPOINT_SCENES + "/" + groupId, endpoint, scheduler);
    }

    @Override
    public @Nullable String getSceneName() {
        TradfriScene sceneData = (TradfriScene) this.cachedData;
        if (sceneData != null) {
            return sceneData.getName();
        }
        return null;
    }

    @Override
    public TradfriScene parsePayload(String coapPayload) {
        return gson.fromJson(coapPayload, TradfriScene.class);
    }
}
