package ai.saniou.thread.domain.usecase.user

import ai.saniou.thread.domain.model.Cookie
import ai.saniou.thread.domain.repository.UserRepository

class DeleteCookieUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(cookie: Cookie) {
        userRepository.deleteCookie(cookie)
    }
}
