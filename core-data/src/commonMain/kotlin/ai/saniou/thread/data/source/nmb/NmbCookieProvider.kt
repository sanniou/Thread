package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.db.Database
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

class NmbAccountProvider(
    private val database: Database
) {
    suspend fun getAccountValue(): String? {
        val cookie = database.accountQueries.getSortedAccounts().asFlow().mapToList(Dispatchers.Default).first().firstOrNull()?.account
        return cookie?.let { "userhash=$it" }
    }
}
