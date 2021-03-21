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
package org.openhab.binding.tradfri.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Configuration class for the gateway.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Jan Möller - Moved configuration property names from TradfriBindingConstants to this class
 */
@NonNullByDefault
public class TradfriGatewayConfig {

    // List of all gateway configuration properties
    public static final String CONFIG_HOST = "host";
    public static final String CONFIG_PORT = "port";
    public static final String CONFIG_CODE = "code";
    public static final String CONFIG_IDENTITY = "identity";
    public static final String CONFIG_PRE_SHARED_KEY = "preSharedKey";

    public @Nullable String host;
    public int port = 5684; // default port
    public @Nullable String code;
    public @Nullable String identity;
    public @Nullable String preSharedKey;
}
