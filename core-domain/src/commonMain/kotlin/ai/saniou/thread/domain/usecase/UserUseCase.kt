package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.model.Cookie
import ai.saniou.thread.domain.repository.UserRepository

class UserUseCase(
    private val userRepository: UserRepository
) {
    suspend fun getCookiesList(): List<Cookie> {
        return userRepository.getCookiesList()
    }

    suspend fun addCookie(name: String, value: String) {
        userRepository.addCookie(
            name = name,
            value = value,
        )
    }

    suspend fun deleteCookie(cookie: Cookie) {
        userRepository.deleteCookie(cookie)
    }

    suspend fun updateCookieSort(cookies: List<Cookie>) {
        userRepository.updateCookieSort(cookies)
    }
}