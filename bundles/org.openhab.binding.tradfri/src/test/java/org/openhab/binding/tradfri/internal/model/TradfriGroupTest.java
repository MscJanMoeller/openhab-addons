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
package org.openhab.binding.tradfri.internal.model;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.gson.Gson;

/**
 * Tests for {@link TradfriGroup}.
 *
 * @author Jan Möller - Initial contribution
 */
public class TradfriGroupTest {

    private final Gson gson = new Gson();

    @Test
    public void testGroupResponse() {
        String json = "{\"9003\":131079," + "\"9001\":\"Living room dining table\"," + "\"9002\":1572085357,"
                + "\"9039\":196635," + "\"5850\":0," + "\"5851\":0," + "\"9108\":0," + "\"9018\":{\"15002\":"
                + "{\"9003\":[65552,65553,65554]}}}";

        TradfriGroup grp = this.gson.fromJson(json, TradfriGroup.class);

        // Check data of class TradfriResource
        assertThat(grp.getInstanceId(), is("131079"));
        assertThat(grp.getName(), is("Living room dining table"));
        assertThat(grp.getTimestampCreatedAt(), is(1572085357L));

        // Check data of class TradfriGroupTest
        assertThat(grp.getSceneId(), is("196635"));
        assertThat(grp.getGroupType(), is(0));
        assertThat(grp.getMembers().size(), is(3));
        assertThat(grp.getMembers().toSet(), contains("65552", "65553", "65554"));
    }
}
