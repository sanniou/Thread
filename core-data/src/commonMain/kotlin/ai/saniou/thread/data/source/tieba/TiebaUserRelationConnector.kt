package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.remote.MiniTiebaApi
import ai.saniou.thread.data.source.tieba.remote.OfficialTiebaApi
import ai.saniou.thread.data.source.tieba.remote.WebTiebaApi
import ai.saniou.thread.domain.source.ProfileEditRequest
import ai.saniou.thread.domain.source.UserRelationConnector
import ai.saniou.thread.domain.source.UserRelationProfile
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

/**
 * Tieba follow/unfollow is portrait-based. We resolve portrait via [MiniTiebaApi.profile]
 * when callers pass a numeric uid (or reuse a portrait string when already provided).
 */
class TiebaUserRelationConnector(
    private val source: TiebaSource,
    private val miniApi: MiniTiebaApi,
    private val officialApi: OfficialTiebaApi,
    private val webApi: WebTiebaApi,
    private val parameterProvider: TiebaParameterProvider,
) : UserRelationConnector {
    override val sourceId: String = source.id

    override suspend fun getProfile(userId: String): Result<UserRelationProfile> = runCatching {
        require(userId.isNotBlank()) { "用户 ID 不能为空" }
        val response = miniApi.profile(uid = userId)
        if (!response.errorCode.isNullOrBlank() && response.errorCode != "0") {
            throw IllegalStateException(response.errorMsg ?: "获取用户资料失败 (${response.errorCode})")
        }
        val user = response.user ?: throw IllegalStateException("用户资料为空")
        val id = user.id?.takeIf(String::isNotBlank) ?: userId
        val name = user.nameShow?.takeIf(String::isNotBlank)
            ?: user.name?.takeIf(String::isNotBlank)
            ?: "贴吧用户"
        UserRelationProfile(
            userId = id,
            name = name,
            avatar = user.portrait?.takeIf(String::isNotBlank)?.let(::portraitUrl),
            intro = user.intro?.takeIf(String::isNotBlank),
            isFollowing = user.hasConcerned == "1",
            fansCount = user.fansNum?.toLongOrNull(),
            followCount = user.concernNum?.toLongOrNull(),
            sex = user.sex?.toIntOrNull(),
        )
    }

    override suspend fun follow(userId: String): Result<String> = runCatching {
        val portrait = resolvePortrait(userId)
        val tbs = parameterProvider.ensureTbs(webApi)
        val stoken = parameterProvider.getSToken().takeIf(String::isNotBlank)
            ?: throw IllegalStateException("请先登录贴吧账号后再关注用户")
        val response = officialApi.follow(
            portrait = portrait,
            tbs = tbs,
            stoken = stoken,
        )
        if (response.errorCode != 0) {
            throw IllegalStateException(response.errorMsg.ifBlank { "关注失败 (${response.errorCode})" })
        }
        response.info.toastText.takeIf(String::isNotBlank) ?: "已关注"
    }

    override suspend fun unfollow(userId: String): Result<String> = runCatching {
        val portrait = resolvePortrait(userId)
        val tbs = parameterProvider.ensureTbs(webApi)
        val stoken = parameterProvider.getSToken().takeIf(String::isNotBlank)
            ?: throw IllegalStateException("请先登录贴吧账号后再取消关注")
        val response = officialApi.unfollow(
            portrait = portrait,
            tbs = tbs,
            stoken = stoken,
        )
        if (response.errorCode != 0) {
            throw IllegalStateException(response.errorMsg.ifBlank { "取消关注失败 (${response.errorCode})" })
        }
        "已取消关注"
    }

    override suspend fun updateProfile(request: ProfileEditRequest): Result<String> = runCatching {
        val stoken = parameterProvider.getSToken().takeIf(String::isNotBlank)
            ?: throw IllegalStateException("请先登录贴吧账号后再修改资料")
        val nick = request.nickName.trim()
        require(nick.isNotBlank()) { "昵称不能为空" }
        val intro = request.intro.trim().take(500)
        val sex = request.sex.coerceIn(0, 2)
        val response = officialApi.profileModify(
            birthdayShowStatus = if (request.birthdayShowStatus) "1" else "0",
            birthdayTime = request.birthdayTimeSec.coerceAtLeast(0L).toString(),
            intro = intro,
            sex = sex.toString(),
            nickName = nick,
            sToken = stoken,
        )
        if (response.errorCode != 0) {
            throw IllegalStateException(response.errorMsg.ifBlank { "修改资料失败 (${response.errorCode})" })
        }
        "资料已更新"
    }

    override suspend fun uploadPortrait(
        fileName: String,
        bytes: ByteArray,
        contentType: String,
    ): Result<String> = runCatching {
        require(parameterProvider.getSToken().isNotBlank()) {
            "请先登录贴吧账号后再修改头像"
        }
        require(bytes.isNotEmpty()) { "头像图片不能为空" }
        require(bytes.size <= MAX_PORTRAIT_BYTES) { "头像不能超过 5 MB" }
        val safeName = fileName.substringAfterLast('/').ifBlank { "file" }
        val mime = contentType.takeIf { it.isNotBlank() && it != "application/octet-stream" }
            ?: mimeFromName(safeName)
        val body = MultiPartFormDataContent(
            formData {
                append("_client_version", CLIENT_VERSION)
                append(
                    key = "pic",
                    value = bytes,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, mime)
                        append(HttpHeaders.ContentDisposition, "filename=\"$safeName\"")
                    },
                )
            },
            boundary = BOUNDARY,
        )
        val response = officialApi.imgPortrait(body = body)
        if (response.errorCode != 0) {
            throw IllegalStateException(
                response.errorMsg.ifBlank { "修改头像失败 (${response.errorCode})" },
            )
        }
        response.errorMsg.takeIf(String::isNotBlank) ?: "头像已更新"
    }

    private suspend fun resolvePortrait(userId: String): String {
        val raw = userId.trim()
        require(raw.isNotBlank()) { "用户 ID 不能为空" }
        // Already a portrait token / full portrait URL path segment.
        if (!raw.all(Char::isDigit)) {
            return raw.substringAfterLast('/').ifBlank { raw }
        }
        val response = miniApi.profile(uid = raw)
        if (!response.errorCode.isNullOrBlank() && response.errorCode != "0") {
            throw IllegalStateException(response.errorMsg ?: "获取用户 portrait 失败 (${response.errorCode})")
        }
        val portrait = response.user?.portrait?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("用户 portrait 为空，无法关注")
        return portrait.substringAfterLast('/')
    }

    private fun portraitUrl(portrait: String): String =
        if (portrait.startsWith("http")) portrait
        else "https://tb.himg.baidu.com/sys/portrait/item/$portrait"

    private fun mimeFromName(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        else -> "application/octet-stream"
    }

    private companion object {
        const val CLIENT_VERSION = "11.10.8.6"
        const val BOUNDARY = "--------7da3d81520810*"
        const val MAX_PORTRAIT_BYTES = 5 * 1024 * 1024
    }
}
