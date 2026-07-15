package ai.saniou.thread.domain.usecase.user

import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.repository.LoginRepository

class LoginSourceUseCase(
    private val repository: LoginRepository,
) {
    suspend operator fun invoke(sourceId: String, inputs: Map<String, String>): Account =
        repository.login(sourceId, inputs)
}
