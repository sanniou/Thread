package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.repository.AccountRepository
import ai.saniou.thread.domain.repository.LoginRepository
import ai.saniou.thread.domain.repository.IdentityRepository
import kotlin.time.Clock
import ai.saniou.thread.domain.source.ConnectorRegistry

class LoginRepositoryImpl(
    private val registry: ConnectorRegistry,
    private val accountRepository: AccountRepository,
    private val identityRepository: IdentityRepository? = null,
) : LoginRepository {
    override suspend fun login(sourceId: String, inputs: Map<String, String>): Account {
        val connector = registry.login(sourceId)
            ?: throw UnsupportedOperationException("Source '$sourceId' does not support login")
        return connector.login(inputs).also { account ->
            require(account.sourceId == sourceId) { "Login connector returned a mismatched source id" }
            accountRepository.addAccount(account)
            identityRepository?.markAuthenticated(sourceId, Clock.System.now().toEpochMilliseconds())
        }
    }
}
