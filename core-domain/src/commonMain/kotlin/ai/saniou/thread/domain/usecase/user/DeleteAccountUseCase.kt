package ai.saniou.thread.domain.usecase.user

import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.repository.UserRepository

class DeleteAccountUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(account: Account) {
        userRepository.deleteAccount(account)
    }
}
