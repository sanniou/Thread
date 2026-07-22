package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.remote.MiniTiebaApi
import ai.saniou.thread.data.source.tieba.remote.OfficialTiebaApi
import ai.saniou.thread.data.source.tieba.remote.WebTiebaApi
import ai.saniou.thread.domain.source.UserRelationConnector
import ai.saniou.thread.domain.source.UserRelationProfile

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
}
