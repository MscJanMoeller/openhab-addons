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
package org.openhab.binding.tradfri.internal.coap.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tradfri.internal.TradfriBindingConstants;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link TradfriCoapGateway} class is used for a data transfer object (DTO) which contains data related to the
 * Tradfri gateway.
 *
 * @author Jan Möller - Initial contribution
 */

@NonNullByDefault
public class TradfriCoapGateway {

    @SerializedName(value = TradfriBindingConstants.ALEXA_PAIR_STATUS)
    private int alexaPairStatus;
    @SerializedName(value = TradfriBindingConstants.CERTIFICATE_PROVISIONED)
    private int certificateProvisioned;
    @SerializedName(value = TradfriBindingConstants.COMMISSIONING_MODE)
    private int commissioningMode;
    @SerializedName(value = TradfriBindingConstants.DST_END_DAY)
    private int dstEndDay;
    @SerializedName(value = TradfriBindingConstants.DST_END_HOUR)
    private int dstEndHour;
    @SerializedName(value = TradfriBindingConstants.DST_END_MINUTE)
    private int dstEndMinute;
    @SerializedName(value = TradfriBindingConstants.DST_END_MONTH)
    private int dstEndMonth;
    @SerializedName(value = TradfriBindingConstants.DST_START_DAY)
    private int dstStartDay;
    @SerializedName(value = TradfriBindingConstants.DST_START_HOUR)
    private int dstStartHour;
    @SerializedName(value = TradfriBindingConstants.DST_START_MINUTE)
    private int dstStartMinute;
    @SerializedName(value = TradfriBindingConstants.DST_START_MONTH)
    private int dstStartMonth;
    @SerializedName(value = TradfriBindingConstants.DST_TIME_OFF_SET)
    private int dstTimeOffSet;
    @SerializedName(value = TradfriBindingConstants.GATEWAY_NAME)
    private @Nullable String gatewayName;
    @SerializedName(value = TradfriBindingConstants.GATEWAY_TIME_SOURCE)
    private int gatewayTimeSource = -1;
    @SerializedName(value = TradfriBindingConstants.GATEWAY_UNIQUE_ID)
    private @Nullable String gatewayUniqueId;
    @SerializedName(value = TradfriBindingConstants.GATEWAY_UPDATE_DETAILS_URL)
    private @Nullable String gatewayUpdateDetailsUrl;
    @SerializedName(value = TradfriBindingConstants.GATEWAY_UPDATE_PROGRESS)
    private int gatewayUpdateProgress;
    @SerializedName(value = TradfriBindingConstants.GOOGLE_HOME_PAIR_STATUS)
    private int googleHomePairStatus;
    @SerializedName(value = TradfriBindingConstants.HOME_KIT_PAIRING_STATUS)
    private int homeKitPairingStatus = -1;
    @SerializedName(value = TradfriBindingConstants.HOME_KIT_SETUP_CODE)
    private @Nullable String homekitSetupCode;
    @SerializedName(value = TradfriBindingConstants.IOT_ENDPOINT)
    private @Nullable String iotEndpoint;
    @SerializedName(value = TradfriBindingConstants.NTP_SERVER_URL)
    private @Nullable String ntpServerUrl;
    @SerializedName(value = TradfriBindingConstants.OTA_FORCE_CHECK_UPDATE)
    private @Nullable String otaForceCheckUpdate;
    @SerializedName(value = TradfriBindingConstants.OTA_UPDATE_STATE)
    private int otaUpdateState;
    @SerializedName(value = TradfriBindingConstants.OTA_UPDATE_TYPE)
    private int otaUpdateType;
    @SerializedName(value = TradfriBindingConstants.SONOS_CERTIFICATE_STATUS)
    private int sonosCertificateStatus;
    @SerializedName(value = TradfriBindingConstants.TIMESTAMP_CURRENT_FORMAT_UNIX)
    private long timestampCurrentFormatUnix;
    @SerializedName(value = TradfriBindingConstants.TIMESTAMP_CURRENT_FORMAT_ISO_8601)
    private @Nullable String timestampCurrentFormatISO8601;
    @SerializedName(value = TradfriBindingConstants.TIMESTAMP_LAST_MODIFICATION)
    private long timestampLastModification;
    @SerializedName(value = TradfriBindingConstants.TIMESTAMP_UPDATE_ACCEPTED)
    private long timestampUpdateAccepted;
    @SerializedName(value = TradfriBindingConstants.VERSION)
    private @Nullable String version;

    public int getAlexaPairStatus() {
        return this.alexaPairStatus;
    }

    public int getCertificateProvisioned() {
        return this.certificateProvisioned;
    }

    public int getCommissioningMode() {
        return this.commissioningMode;
    }

    public int getDstEndDay() {
        return dstEndDay;
    }

    public int getDstEndHour() {
        return dstEndHour;
    }

    public int getDstEndMinute() {
        return dstEndMinute;
    }

    public int getDstEndMonth() {
        return dstEndMonth;
    }

    public int getDstStartDay() {
        return dstStartDay;
    }

    public int getDstStartHour() {
        return dstStartHour;
    }

    public int getDstStartMinute() {
        return dstStartMinute;
    }

    public int getDstStartMonth() {
        return dstStartMonth;
    }

    public int getDstTimeOffSet() {
        return dstTimeOffSet;
    }

    public @Nullable String getGatewayName() {
        return gatewayName;
    }

    public int getGatewayTimeSource() {
        return gatewayTimeSource;
    }

    public @Nullable String getGatewayUniqueId() {
        return gatewayUniqueId;
    }

    public @Nullable String getGatewayUpdateDetailsUrl() {
        return gatewayUpdateDetailsUrl;
    }

    public int getGatewayUpdateProgress() {
        return gatewayUpdateProgress;
    }

    public int getGoogleHomePairStatus() {
        return this.googleHomePairStatus;
    }

    public int getHomeKitPairingStatus() {
        return this.homeKitPairingStatus;
    }

    public @Nullable String getHomekitSetupCode() {
        return homekitSetupCode;
    }

    public @Nullable String getIotEndpoint() {
        return this.iotEndpoint;
    }

    public @Nullable String getNtpServerUrl() {
        return ntpServerUrl;
    }

    public @Nullable String getOtaForceCheckUpdate() {
        return otaForceCheckUpdate;
    }

    public int getOtaUpdateState() {
        return otaUpdateState;
    }

    public int getOtaUpdateType() {
        return otaUpdateType;
    }

    public int getSonosCertificateStatus() {
        return this.sonosCertificateStatus;
    }

    public long getTimestampCurrentFormatUnix() {
        return timestampCurrentFormatUnix;
    }

    public @Nullable String getTimestampCurrentFormatISO8601() {
        return timestampCurrentFormatISO8601;
    }

    public long getTimestampLastModification() {
        return timestampLastModification;
    }

    public long getTimestampUpdateAccepted() {
        return this.timestampUpdateAccepted;
    }

    public @Nullable String getVersion() {
        return this.version;
    }
}
