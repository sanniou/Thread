package ai.saniou.thread.domain.usecase.user

import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.repository.UserRepository

class GetAccountsUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(): List<Account> {
        return userRepository.getAccountsList()
    }
}
