package ai.saniou.thread.data.source.nmb

import ai.saniou.nmb.db.Database
import ai.saniou.thread.network.CookieProvider
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

class NmbCookieProvider(
    private val database: Database
) : CookieProvider {
    override suspend fun getCookieValue(): String? {
        return database.cookieQueries.getSortedCookies().asFlow().mapToList(Dispatchers.Default).first().firstOrNull()?.cookie
    }
}
