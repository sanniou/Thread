package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.remote.WebTiebaApi

/**
 * Shared credential helpers for Tieba write APIs (post, store, like-forum, sign).
 */
internal suspend fun TiebaParameterProvider.ensureTbs(webApi: WebTiebaApi): String {
    getTbs().takeIf(String::isNotBlank)?.let { return it }
    val profile = webApi.myInfo("BDUSS=${getBduss()}; STOKEN=${getSToken()}")
    require(profile.isLogin && profile.tbs.isNotBlank()) { "贴吧登录已失效，无法刷新 TBS" }
    updateCredentials(
        bduss = getBduss(),
        stoken = getSToken(),
        uid = profile.uid.toString(),
        tbs = profile.tbs,
    )
    return profile.tbs
}

internal fun portraitUrl(portrait: String?): String? {
    val value = portrait?.takeIf(String::isNotBlank) ?: return null
    return if (value.startsWith("http")) value
    else "https://tb.himg.baidu.com/sys/portrait/item/$value"
}
