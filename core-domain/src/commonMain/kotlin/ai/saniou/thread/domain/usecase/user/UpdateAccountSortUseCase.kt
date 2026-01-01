package ai.saniou.thread.domain.usecase.user

import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.repository.UserRepository

class UpdateAccountSortUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(accounts: List<Account>) {
        userRepository.updateAccountSort(accounts)
    }
}
