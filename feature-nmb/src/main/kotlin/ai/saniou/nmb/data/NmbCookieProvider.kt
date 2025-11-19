package ai.saniou.nmb.data

import ai.saniou.corecommon.data.CookieProvider
import ai.saniou.nmb.data.repository.NmbRepository

class NmbCookieProvider(
    private val nmbRepository: NmbRepository
) : CookieProvider {
    override suspend fun getCookieValue(): String? {
        return nmbRepository.getSortedCookies().firstOrNull()?.cookie
    }
}