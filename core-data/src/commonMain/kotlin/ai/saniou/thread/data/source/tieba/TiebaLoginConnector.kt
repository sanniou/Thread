package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.nmb.required
import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.source.LoginConnector
import kotlin.time.Clock

class TiebaLoginConnector(
    private val source: TiebaSource,
) : LoginConnector {
    override val sourceId: String = source.id
    override val strategy: LoginStrategy = source.loginStrategy

    override suspend fun login(inputs: Map<String, String>): Account {
        val cookieValues = parseCookie(inputs["cookie"].orEmpty()) + inputs
        val bduss = cookieValues.required("BDUSS")
        val stoken = cookieValues.required("STOKEN")
        val now = Clock.System.now()
        return Account(
            id = "$sourceId:${bduss.hashCode()}",
            sourceId = sourceId,
            alias = inputs["alias"].orEmpty().ifBlank { "贴吧账号" },
            value = bduss,
            uid = null,
            avatar = null,
            extraData = """{"stoken":"${stoken.jsonEscape()}","tbs":"","portrait":""}""",
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

private fun String.jsonEscape(): String = replace("\\", "\\\\").replace("\"", "\\\"")
