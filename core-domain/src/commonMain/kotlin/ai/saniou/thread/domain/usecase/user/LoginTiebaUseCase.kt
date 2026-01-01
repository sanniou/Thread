package ai.saniou.thread.domain.usecase.user

import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.repository.AccountRepository
import kotlin.time.Clock

class LoginTiebaUseCase(
    private val accountRepository: AccountRepository,
) {
    /**
     * @param bduss BDUSS from WebView or login response
     * @param sToken SToken from WebView or login response
     * @param uid User ID
     * @param name User name
     * @param portrait User portrait/avatar hash
     * @param tbs TBS
     */
    suspend operator fun invoke(
        bduss: String,
        sToken: String,
        uid: String,
        name: String,
        portrait: String,
        tbs: String
    ) {
        val now = Clock.System.now()
        // Construct Account
        val account = Account(
            id = uid, // Use UID as ID for Tieba accounts
            sourceId = "tieba",
            value = bduss, // BDUSS is the main cookie
            uid = uid,
            alias = name,
            avatar = "http://tb.himg.baidu.com/sys/portrait/item/$portrait",
            extraData = buildExtraData(sToken, tbs, portrait),
            sort = 0,
            isCurrent = true,
            createdAt = now,
            lastUsedAt = now
        )

        accountRepository.addAccount(account)
    }

    private fun buildExtraData(sToken: String, tbs: String, portrait: String): String {
        // Simple JSON construction manually to avoid dependency in this snippet if not needed,
        // or use kotlinx.serialization if available in domain.
        // Assuming kotlinx.serialization is available in core-domain or we use string templates carefully.
        return """{"stoken":"$sToken","tbs":"$tbs","portrait":"$portrait"}"""
    }
}
