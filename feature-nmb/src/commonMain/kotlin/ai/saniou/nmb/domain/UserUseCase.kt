package ai.saniou.nmb.domain

import ai.saniou.nmb.db.table.Cookie
import ai.saniou.thread.data.source.nmb.NmbSource

class UserUseCase(
    private val nmbRepository: NmbSource
) {
    suspend fun getCookiesList(): List<Cookie> {
        return nmbRepository.getSortedCookies()
    }

    suspend fun addCookie(name: String, value: String) {
        nmbRepository.insertCookie(
            alias = name,
            cookie = value,
        )
    }

    suspend fun deleteCookie(cookie: Cookie) {
        nmbRepository.deleteCookie(cookie.cookie)
    }

    suspend fun updateCookieSort(cookies: List<Cookie>) {
        nmbRepository.updateCookiesSort(cookies)
    }
}
