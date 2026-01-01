package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccounts(sourceId: String): Flow<List<Account>>
    fun getCurrentAccount(sourceId: String): Flow<Account?>
    suspend fun addAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(id: String)
    suspend fun switchAccount(sourceId: String, id: String)
}