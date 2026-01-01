package ai.saniou.thread.domain.usecase.user

import ai.saniou.thread.domain.repository.UserRepository

class AddAccountUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(name: String, value: String) {
        userRepository.addAccount(name, value)
    }
}
