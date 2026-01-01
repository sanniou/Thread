package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Account

interface UserRepository {
    suspend fun getAccountsList(): List<Account>
    suspend fun addAccount(name: String, value: String)
    suspend fun deleteAccount(account: Account)
    suspend fun updateAccountSort(accounts: List<Account>)
}
