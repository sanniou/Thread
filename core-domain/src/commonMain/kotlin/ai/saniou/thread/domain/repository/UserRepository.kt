package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Cookie

interface UserRepository {
    suspend fun getCookiesList(): List<Cookie>
    suspend fun addCookie(name: String, value: String)
    suspend fun deleteCookie(cookie: Cookie)
    suspend fun updateCookieSort(cookies: List<Cookie>)
}