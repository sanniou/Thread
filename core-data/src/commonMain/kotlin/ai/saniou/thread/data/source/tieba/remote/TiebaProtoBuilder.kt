package ai.saniou.thread.data.source.tieba.remote

import ai.saniou.corecommon.utils.toMD5
import ai.saniou.thread.data.source.tieba.TiebaParameterProvider
import ai.saniou.thread.network.tieba.StParamUtils
import com.huanchengfly.tieba.post.api.models.protos.AppPosInfo
import com.huanchengfly.tieba.post.api.models.protos.CommonRequest
import com.huanchengfly.tieba.post.api.models.protos.frsPage.AdParam
import com.squareup.wire.Message
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlin.time.Clock

object TiebaProtoBuilder {

    private const val BOUNDARY = "--------7da3d81520810*"

    fun buildProtobufFormBody(
        data: Message<*, *>,
        clientVersion: ClientVersion = ClientVersion.TIEBA_V11,
        needSToken: Boolean = true,
        parameterProvider: TiebaParameterProvider
    ): MultiPartFormDataContent {
        val params = mutableListOf<Pair<String, String>>()

        // Params Order is important for Sign Calculation or Server Parsing sometimes.
        // Based on tiebapost-success.md:
        // BDUSS, _client_id, _client_type, _client_version, _phone_imei, c3_aid, cuid, cuid_galaxy2, cuid_gid, from, model, net_type, oaid, stoken, timestamp

        params.add("BDUSS" to parameterProvider.getBduss())
        params.add("_client_id" to parameterProvider.getClientId())
        params.add("_client_type" to "2")
        params.add("_client_version" to clientVersion.version)
        params.add("_phone_imei" to "000000000000000") // TODO: Get IMEI

        // Specific params per version logic
        if (clientVersion == ClientVersion.TIEBA_V11) {
             params.add("c3_aid" to parameterProvider.getAndroidId())
        } else {
             // V12 logic
             params.add("c3_aid" to parameterProvider.getAndroidId())
        }

        params.add("cuid" to parameterProvider.getCuid())
        params.add("cuid_galaxy2" to parameterProvider.getCuid())
        params.add("cuid_gid" to "")
        params.add("from" to "tieba")
        params.add("model" to parameterProvider.getModel())
        params.add("net_type" to "1")
        params.add("oaid" to parameterProvider.getOaid())

        if (needSToken) {
            val sToken = parameterProvider.getSToken()
            if (sToken.isNotEmpty()) params.add("stoken" to sToken)
        }

        params.add("timestamp" to parameterProvider.getTimestamp())

        // Note: StParams are NOT added for Multipart Requests in Retrofit implementation (only for FormBody/Query).
        // Removing StParamUtils call to match success request.

        // Calculate sign
        val sortedString = params.sortedBy { it.first }.joinToString("") { "${it.first}=${it.second}" }
        val sign = (sortedString + "tiebaclient!!!").toMD5()
        params.add("sign" to sign)

        return MultiPartFormDataContent(
            formData {
                params.forEach { (key, value) ->
                    append(key, value)
                }

                // Add the protobuf data as a file part named "data"
                val encodedData = data.encode()
                append("data", encodedData, Headers.build {
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"data\"; filename=\"file\"")
                })
            },
            boundary = BOUNDARY
        )
    }

    fun buildAdParam(
        load_count: Int = 0,
        refresh_count: Int = 4,
        yoga_lib_version: String? = "1.0"
    ): AdParam {
        return AdParam(
            load_count = load_count,
            refresh_count = refresh_count,
            yoga_lib_version = yoga_lib_version
        )
    }

    fun buildAppPosInfo(): AppPosInfo {
        return AppPosInfo(
            addr_timestamp = 0L,
            ap_connected = true,
            ap_mac = "02:00:00:00:00:00",
            asp_shown_info = "",
            coordinate_type = "BD09LL"
        )
    }

    fun buildCommonRequest(
        parameterProvider: TiebaParameterProvider,
        clientVersion: ClientVersion = ClientVersion.TIEBA_V11,
        bduss: String? = null,
        stoken: String? = null,
        tbs: String? = null,
    ): CommonRequest {
        val currentBduss = bduss ?: parameterProvider.getBduss()
        val currentSToken = stoken ?: parameterProvider.getSToken()
        val timestamp = parameterProvider.getTimestamp().toLongOrNull() ?: 0L
        val cuid = parameterProvider.getCuid()
        val clientId = parameterProvider.getClientId()

        return when (clientVersion) {
            ClientVersion.TIEBA_V11 -> {
                CommonRequest(
                    BDUSS = currentBduss,
                    _client_id = clientId,
                    _client_type = 2,
                    _client_version = clientVersion.version,
                    _os_version = "30", // SDK 30 (Android 11)
                    _phone_imei = "000000000000000",
                    _timestamp = timestamp,
                    brand = parameterProvider.getBrand(),
                    c3_aid = parameterProvider.getAndroidId(),
                    cuid = cuid,
                    cuid_galaxy2 = cuid,
                    cuid_gid = "",
                    from = "1024324o",
                    is_teenager = 0,
                    lego_lib_version = "3.0.0",
                    model = parameterProvider.getModel(),
                    net_type = 1,
                    oaid = parameterProvider.getOaid(),
                    pversion = "1.0.3",
                    sample_id = parameterProvider.getSampleId(),
                    stoken = currentSToken,
                )
            }

            ClientVersion.TIEBA_V12 -> {
                CommonRequest(
                    BDUSS = currentBduss,
                    _client_id = clientId,
                    _client_type = 2,
                    _client_version = clientVersion.version,
                    _os_version = "30",
                    _phone_imei = "000000000000000",
                    _timestamp = timestamp,
                    active_timestamp = parameterProvider.getActiveTimestamp().toLongOrNull() ?: timestamp,
                    android_id = parameterProvider.getAndroidId(base64 = true),
                    brand = parameterProvider.getBrand(),
                    c3_aid = parameterProvider.getAndroidId(),
                    cmode = 1,
                    cuid = cuid,
                    cuid_galaxy2 = cuid,
                    cuid_gid = "",
                    event_day = "", // TODO: Date Formatting
                    extra = "",
                    first_install_time = parameterProvider.getAppFirstInstallTime().toLongOrNull() ?: timestamp,
                    framework_ver = "3340042",
                    from = "1020031h",
                    is_teenager = 0,
                    last_update_time = parameterProvider.getAppLastUpdateTime().toLongOrNull() ?: timestamp,
                    lego_lib_version = "3.0.0",
                    model = parameterProvider.getModel(),
                    net_type = 1,
                    oaid = "", // V12 uses empty string for OAID in CommonRequest?
                    personalized_rec_switch = 1,
                    pversion = "1.0.3",
                    q_type = 0,
                    sample_id = parameterProvider.getSampleId(),
                    scr_dip = parameterProvider.getScreenDensity(),
                    scr_h = parameterProvider.getScreenHeight(),
                    scr_w = parameterProvider.getScreenWidth(),
                    sdk_ver = "2.34.0",
                    start_scheme = "",
                    start_type = 1,
                    stoken = currentSToken,
                    swan_game_ver = "1038000",
                    user_agent = "tieba/${clientVersion.version}",
                    z_id = parameterProvider.getZid()
                )
            }

            ClientVersion.TIEBA_V12_POST -> {
                CommonRequest(
                    BDUSS = currentBduss,
                    _client_id = clientId,
                    _client_type = 2,
                    _client_version = clientVersion.version,
                    _os_version = "30",
                    _phone_imei = "000000000000000",
                    _timestamp = timestamp,
                    active_timestamp = parameterProvider.getActiveTimestamp().toLongOrNull() ?: timestamp,
                    android_id = parameterProvider.getAndroidId(), // V12 Post seems to use raw Android ID in Retrofit? "UIDUtil.getAndroidId("000")"
                    applist = "",
                    brand = parameterProvider.getBrand(),
                    c3_aid = parameterProvider.getAndroidId(),
                    cmode = 1,
                    cuid = cuid,
                    cuid_galaxy2 = cuid,
                    cuid_gid = "",
                    device_score = parameterProvider.getDeviceScore(),
                    event_day = "",
                    extra = "",
                    first_install_time = parameterProvider.getAppFirstInstallTime().toLongOrNull() ?: timestamp,
                    framework_ver = "3340042",
                    from = "1020031h",
                    is_teenager = 0,
                    last_update_time = parameterProvider.getAppLastUpdateTime().toLongOrNull() ?: timestamp,
                    lego_lib_version = "3.0.0",
                    model = parameterProvider.getModel(),
                    net_type = 1,
                    oaid = parameterProvider.getOaid(),
                    personalized_rec_switch = 1,
                    pversion = "1.0.3",
                    q_type = 0,
                    sample_id = parameterProvider.getSampleId(),
                    scr_dip = parameterProvider.getScreenDensity(),
                    scr_h = parameterProvider.getScreenHeight(),
                    scr_w = parameterProvider.getScreenWidth(),
                    sdk_ver = "2.34.0",
                    start_scheme = "",
                    start_type = 1,
                    stoken = currentSToken,
                    swan_game_ver = "1038000",
                    tbs = tbs,
                    user_agent = "tieba/${clientVersion.version}",
                    z_id = parameterProvider.getZid()
                )
            }
        }
    }
}
