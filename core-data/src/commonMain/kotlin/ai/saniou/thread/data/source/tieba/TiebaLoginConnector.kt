package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.nmb.required
import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.source.LoginConnector
import ai.saniou.thread.data.source.tieba.remote.WebTiebaApi
import kotlin.time.Clock

class TiebaLoginConnector(
    private val source: TiebaSource,
    private val webApi: WebTiebaApi,
    private val parameterProvider: TiebaParameterProvider,
) : LoginConnector {
    override val sourceId: String = source.id
    override val strategy: LoginStrategy = source.loginStrategy

    override suspend fun login(inputs: Map<String, String>): Account {
        val cookieValues = parseCookie(inputs["cookie"].orEmpty()) + inputs
        val bduss = cookieValues.required("BDUSS")
        val stoken = cookieValues.required("STOKEN")
        parameterProvider.updateCredentials(bduss = bduss, stoken = stoken)
        val cookie = "BDUSS=$bduss; STOKEN=$stoken"
        val profile = webApi.myInfo(cookie)
        require(profile.isLogin) { "贴吧 Cookie 未登录或已失效" }
        require(profile.uid > 0) { "贴吧账号信息缺少 UID" }
        val uid = profile.uid.toString()
        val alias = profile.showName.ifBlank { profile.name }.ifBlank { "贴吧账号" }
        val portrait = profile.avatarUrl
        parameterProvider.updateCredentials(
            bduss = bduss,
            stoken = stoken,
            uid = uid,
            tbs = profile.tbs,
        )
        val now = Clock.System.now()
        return Account(
            id = "$sourceId:$uid",
            sourceId = sourceId,
            alias = inputs["alias"].orEmpty().ifBlank { alias },
            value = bduss,
            uid = uid,
            avatar = portrait,
            extraData = TiebaAccountMetadata(
                stoken = stoken,
                tbs = profile.tbs,
                portrait = portrait,
            ).serialize(),
            sort = 0,
            isCurrent = true,
            lastUsedAt = now,
            createdAt = now,
        )
    }
}

private fun parseCookie(cookie: String): Map<String, String> = cookie.split(';')
    .mapNotNull { part ->
        val index = part.indexOf('=')
        if (index <= 0) null else part.substring(0, index).trim() to part.substring(index + 1).trim()
    }
    .toMap()
