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

import org.openhab.binding.tradfri.internal.TradfriBindingConstants;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link TradfriGatewayData} class is a Java wrapper for raw JSON data related to the Tradfri gateway.
 *
 * @author Jan Möller - Initial contribution
 */

public class TradfriGatewayData {

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
    private String gatewayName;
    @SerializedName(value = TradfriBindingConstants.GATEWAY_TIME_SOURCE)
    private int gatewayTimeSource = -1;
    @SerializedName(value = TradfriBindingConstants.GATEWAY_UNIQUE_ID)
    private String gatewayUniqueId;
    @SerializedName(value = TradfriBindingConstants.GATEWAY_UPDATE_DETAILS_URL)
    private String gatewayUpdateDetailsUrl;
    @SerializedName(value = TradfriBindingConstants.GATEWAY_UPDATE_PROGRESS)
    private int gatewayUpdateProgress;
    @SerializedName(value = TradfriBindingConstants.GOOGLE_HOME_PAIR_STATUS)
    private int googleHomePairStatus;
    @SerializedName(value = TradfriBindingConstants.HOME_KIT_PAIRING_STATUS)
    private int homeKitPairingStatus = -1;
    @SerializedName(value = TradfriBindingConstants.HOME_KIT_SETUP_CODE)
    private String homekitSetupCode;
    @SerializedName(value = TradfriBindingConstants.IOT_ENDPOINT)
    private String iotEndpoint;
    @SerializedName(value = TradfriBindingConstants.NTP_SERVER_URL)
    private String ntpServerUrl;
    @SerializedName(value = TradfriBindingConstants.OTA_FORCE_CHECK_UPDATE)
    private String otaForceCheckUpdate;
    @SerializedName(value = TradfriBindingConstants.OTA_UPDATE_STATE)
    private int otaUpdateState;
    @SerializedName(value = TradfriBindingConstants.OTA_UPDATE_TYPE)
    private int otaUpdateType;
    @SerializedName(value = TradfriBindingConstants.SONOS_CERTIFICATE_STATUS)
    private int sonosCertificateStatus;
    @SerializedName(value = TradfriBindingConstants.TIMESTAMP_CURRENT_FORMAT_UNIX)
    private long timestampCurrentFormatUnix;
    @SerializedName(value = TradfriBindingConstants.TIMESTAMP_CURRENT_FORMAT_ISO_8601)
    private String timestampCurrentFormatISO8601;
    @SerializedName(value = TradfriBindingConstants.TIMESTAMP_LAST_MODIFICATION)
    private long timestampLastModification;
    @SerializedName(value = TradfriBindingConstants.TIMESTAMP_UPDATE_ACCEPTED)
    private long timestampUpdateAccepted;
    @SerializedName(value = TradfriBindingConstants.VERSION)
    private String version;

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

    public String getGatewayName() {
        return gatewayName;
    }

    public int getGatewayTimeSource() {
        return gatewayTimeSource;
    }

    public String getGatewayUniqueId() {
        return gatewayUniqueId;
    }

    public String getGatewayUpdateDetailsUrl() {
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

    public String getHomekitSetupCode() {
        return homekitSetupCode;
    }

    public String getIotEndpoint() {
        return this.iotEndpoint;
    }

    public String getNtpServerUrl() {
        return ntpServerUrl;
    }

    public String getOtaForceCheckUpdate() {
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

    public String getTimestampCurrentFormatISO8601() {
        return timestampCurrentFormatISO8601;
    }

    public long getTimestampLastModification() {
        return timestampLastModification;
    }

    public long getTimestampUpdateAccepted() {
        return this.timestampUpdateAccepted;
    }

    public String getVersion() {
        return this.version;
    }
}
