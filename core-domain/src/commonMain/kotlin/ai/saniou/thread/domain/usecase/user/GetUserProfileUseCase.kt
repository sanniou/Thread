package ai.saniou.thread.domain.usecase.user

import ai.saniou.thread.domain.model.forum.Cookie
import ai.saniou.thread.domain.repository.UserRepository

class GetUserProfileUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(): List<Cookie> {
        return userRepository.getCookiesList()
    }
}
