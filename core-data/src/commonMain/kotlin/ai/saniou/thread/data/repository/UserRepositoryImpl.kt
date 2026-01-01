package ai.saniou.thread.data.repository

import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.repository.UserRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import ai.saniou.thread.db.table.Account as TableAccount

@OptIn(ExperimentalTime::class)
class UserRepositoryImpl(
    private val db: Database,
) : UserRepository {
    override suspend fun getAccountsList(): List<Account> {
        return getSortedAccounts().toDomain()
    }

    override suspend fun addAccount(name: String, value: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        val count =
            db.accountQueries.countAccounts().asFlow().mapToList(Dispatchers.Default).first().size
        db.accountQueries.insertAccount(
            id = value, // Use cookie string as ID for NMB compatibility
            source_id = "nmb",
            account = value,
            uid = null,
            alias = name,
            avatar = null,
            extra_data = null,
            sort = count.toLong(),
            is_current = 0L,
            createdAt = now,
            lastUsedAt = now
        )
    }

    override suspend fun deleteAccount(account: Account) {
        db.accountQueries.deleteAccount(account.id)
    }

    override suspend fun updateAccountSort(accounts: List<Account>) {
        db.accountQueries.transaction {
            accounts.forEachIndexed { index, account ->
                db.accountQueries.updateAccountSort(index.toLong(), account.id)
            }
        }
    }

    private suspend fun getSortedAccounts(): List<TableAccount> {
        return db.accountQueries.getSortedAccounts().asFlow().mapToList(Dispatchers.Default).first()
    }

    suspend fun getAccountValue(): String? {
        return getSortedAccounts().firstOrNull()?.account
    }
}
