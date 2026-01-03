package ai.saniou.thread.network.tieba

import ai.saniou.corecommon.utils.toMD5
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.FormPart
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.content.PartData
import io.ktor.http.content.TextContent
import io.ktor.util.AttributeKey
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.core.writeFully

// 移植自 RetrofitTiebaApi 的常量
object TiebaApiConstants {
    const val FORCE_PARAM = "Force-Param"
    const val FORCE_PARAM_QUERY = "1"
    const val NO_COMMON_PARAMS = "No-Common-Params"

    const val SIGN = "sign"
    const val CLIENT_VERSION = "client_version"
}

class TiebaCommonPluginsConfig {
    var headers: Map<String, () -> String?> = emptyMap()
    var params: Map<String, () -> String?> = emptyMap()
    var appSecret: String = ""
}

val TiebaCommonHeaderPlugin = createClientPlugin("TiebaCommonHeaderPlugin", ::TiebaCommonPluginsConfig) {
    val headers = pluginConfig.headers

    onRequest { request, _ ->
        headers.forEach { (key, valueProvider) ->
            if (!request.headers.contains(key)) {
                val value = valueProvider()
                if (value != null) {
                    request.headers[key] = value
                }
            }
        }
    }
}

val TiebaCommonParamPlugin = createClientPlugin("TiebaCommonParamPlugin", ::TiebaCommonPluginsConfig) {
    val params = pluginConfig.params

    // Transform request to add params
    transformRequestBody { request, content, _ ->
        // Check for NO_COMMON_PARAMS header
        val noCommonParams = request.headers[TiebaApiConstants.NO_COMMON_PARAMS]?.split(",") ?: emptyList()
        // Remove the custom header so it doesn't go to the network
        if (request.headers.contains(TiebaApiConstants.NO_COMMON_PARAMS)) {
            request.headers.remove(TiebaApiConstants.NO_COMMON_PARAMS)
        }

        val forceQuery = request.headers[TiebaApiConstants.FORCE_PARAM] == TiebaApiConstants.FORCE_PARAM_QUERY
        if (request.headers.contains(TiebaApiConstants.FORCE_PARAM)) {
            request.headers.remove(TiebaApiConstants.FORCE_PARAM)
        }

        // Add to Query for GET or ForceQuery
        if (request.method == HttpMethod.Get || forceQuery) {
            params.forEach { (key, valueProvider) ->
                if (!request.url.parameters.contains(key) && !noCommonParams.contains(key)) {
                    val value = valueProvider()
                    if (!value.isNullOrEmpty()) {
                        request.url.parameters.append(key, value)
                    }
                }
            }
            return@transformRequestBody null // Return null to keep original body (which is likely null or empty for GET)
        }

        // Handle Form Body
        if (content is FormDataContent) {
            val oldParams = content.formData
            val newParamsBuilder = ParametersBuilder()

            // Copy old params
            oldParams.forEach { key, values ->
                newParamsBuilder.appendAll(key, values)
            }

            // Add new params
            params.forEach { (key, valueProvider) ->
                if (!oldParams.contains(key) && !noCommonParams.contains(key)) {
                    val value = valueProvider()
                    if (!value.isNullOrEmpty()) {
                        newParamsBuilder.append(key, value)
                    }
                }
            }

            return@transformRequestBody FormDataContent(newParamsBuilder.build())
        }

        // Handle Multipart Body (simplified, assuming we can rebuild it)
        if (content is MultiPartFormDataContent) {
            // Handling Multipart is complex because we need to read parts.
            // Ktor's MultiPartFormDataContent doesn't easily expose parts list for modification without reading stream.
            // For now, we skip Multipart modification in this plugin or need a more complex approach.
            // Given Tieba usage, FormUrlEncoded is most common for params.
            // Retrofit implementation adds parts.

            // TODO: Implement Multipart support if needed.
        }

        // If body is empty/null but method is POST, we might want to create a form body?
        // But transformRequestBody receives the content. If content is null (EmptyContent), we can return a new one.
        // However, Ktor might not trigger transformRequestBody for EmptyContent in some versions or configurations.
        // Checking request.body directly in onRequest might be better for empty bodies.

        null
    }

    onRequest { request, content ->
        // Handle empty body for POST to add common params as Form
        if (request.method == HttpMethod.Post && request.bodyType == null && content is io.ktor.client.utils.EmptyContent) {
            val noCommonParams = request.headers[TiebaApiConstants.NO_COMMON_PARAMS]?.split(",") ?: emptyList()

            val newParamsBuilder = ParametersBuilder()
            params.forEach { (key, valueProvider) ->
                if (!noCommonParams.contains(key)) {
                    val value = valueProvider()
                    if (!value.isNullOrEmpty()) {
                        newParamsBuilder.append(key, value)
                    }
                }
            }
            if (!newParamsBuilder.isEmpty()) {
                request.setBody(FormDataContent(newParamsBuilder.build()))
            }
        }
    }
}

val TiebaSortAndSignPlugin = createClientPlugin("TiebaSortAndSignPlugin", ::TiebaCommonPluginsConfig) {
    val appSecret = pluginConfig.appSecret

    transformRequestBody { request, content, _ ->
        // Sign logic
        // Case 1: Query contains BDUSS but no sign -> Sign Query
        if (request.url.parameters.contains("BDUSS") && !request.url.parameters.contains(TiebaApiConstants.SIGN)) {
            val sortedQuery = request.url.parameters.entries()
                .flatMap { (k, v) -> v.map { k to it } }
                .sortedBy { it.first }
                .joinToString("") { "${it.first}=${it.second}" }

            // Note: Retrofit implementation uses decoded query for calculation?
            // "url.query!!.split('&')" suggests raw query.
            // But here we construct from parameters.
            // Let's follow the standard: name=value

            val sign = (sortedQuery + appSecret).toMD5()
            request.url.parameters.append(TiebaApiConstants.SIGN, sign)
            return@transformRequestBody null
        }

        // Case 2: Form Body
        if (content is FormDataContent) {
            val params = content.formData
            if (params.contains(TiebaApiConstants.CLIENT_VERSION) && !params.contains(TiebaApiConstants.SIGN)) {
                val sortedRaw = params.entries()
                    .flatMap { (k, v) -> v.map { k to it } }
                    .sortedBy { it.first }
                    .joinToString("") { "${it.first}=${it.second}" }

                val sign = (sortedRaw + appSecret).toMD5()

                val newParams = ParametersBuilder().apply {
                    appendAll(params)
                    append(TiebaApiConstants.SIGN, sign)
                }
                return@transformRequestBody FormDataContent(newParams.build())
            }
        }

        null
    }
}
