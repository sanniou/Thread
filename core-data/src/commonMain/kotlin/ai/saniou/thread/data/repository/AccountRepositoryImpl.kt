package ai.saniou.thread.data.repository

import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.Account as TableAccount
import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.repository.AccountRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class AccountRepositoryImpl(
    private val db: Database,
) : AccountRepository {

    override fun getAccounts(sourceId: String): Flow<List<Account>> {
        return db.accountQueries.getAccountsBySource(sourceId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.toDomain() }
    }

    override fun getCurrentAccount(sourceId: String): Flow<Account?> {
        return db.accountQueries.getCurrentAccount(sourceId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain() }
    }

    override suspend fun addAccount(account: Account) {
        val now = Clock.System.now().toEpochMilliseconds()
        // If sort is 0 (default), try to append to end
        val sort = if (account.sort == 0L) {
            val count = db.accountQueries.countAccounts().executeAsList().size
            count.toLong()
        } else {
            account.sort
        }

        db.accountQueries.transaction {
            // If this is the first account for this source, make it current
            val existingCount = db.accountQueries.getAccountsBySource(account.sourceId).executeAsList().size
            val isCurrent = if (existingCount == 0) 1L else if (account.isCurrent) 1L else 0L

            if (isCurrent == 1L) {
                db.accountQueries.clearCurrentAccount(account.sourceId)
            }

            db.accountQueries.insertAccount(
                id = account.id,
                source_id = account.sourceId,
                account = account.value,
                uid = account.uid,
                alias = account.alias,
                avatar = account.avatar,
                extra_data = account.extraData,
                sort = sort,
                is_current = isCurrent,
                createdAt = account.createdAt.toEpochMilliseconds(),
                lastUsedAt = now // Treat add as use
            )
        }
    }

    override suspend fun updateAccount(account: Account) {
        db.accountQueries.updateAccount(
            account = account.value,
            uid = account.uid,
            alias = account.alias,
            avatar = account.avatar,
            extra_data = account.extraData,
            sort = account.sort,
            lastUsedAt = account.lastUsedAt.toEpochMilliseconds(),
            id = account.id
        )
    }

    override suspend fun deleteAccount(id: String) {
        db.accountQueries.deleteAccount(id)
    }

    override suspend fun switchAccount(sourceId: String, id: String) {
        db.accountQueries.transaction {
            db.accountQueries.clearCurrentAccount(sourceId)
            db.accountQueries.setCurrentAccount(id)
        }
    }
}
