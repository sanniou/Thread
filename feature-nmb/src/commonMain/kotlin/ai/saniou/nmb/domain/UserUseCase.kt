package ai.saniou.nmb.domain

import ai.saniou.nmb.data.repository.NmbRepository
import ai.saniou.nmb.db.table.Cookie

class UserUseCase(
    private val nmbRepository: NmbRepository
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
