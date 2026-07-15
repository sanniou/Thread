package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.domain.repository.AccountRepository
import kotlinx.coroutines.flow.first

class NmbAccountProvider(
    private val accountRepository: AccountRepository,
) {
    suspend fun getAccountValue(): String? {
        val account = accountRepository.getCurrentAccount(NMBSourceId).first()
            ?: accountRepository.getAccounts(NMBSourceId).first().firstOrNull()
        return account?.value?.let { value ->
            if (value.startsWith("userhash=")) value else "userhash=$value"
        }
    }
}
