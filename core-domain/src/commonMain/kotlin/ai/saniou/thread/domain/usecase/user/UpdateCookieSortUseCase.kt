package ai.saniou.thread.domain.usecase.user

import ai.saniou.thread.domain.model.Cookie
import ai.saniou.thread.domain.repository.UserRepository

class UpdateCookieSortUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(cookies: List<Cookie>) {
        userRepository.updateCookieSort(cookies)
    }
}
