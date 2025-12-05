package ai.saniou.nmb.data

import ai.saniou.thread.network.CookieProvider
import ai.saniou.nmb.db.Database
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

class NmbCookieProvider(
    private val database: Database
) : CookieProvider {
    override suspend fun getCookieValue(): String? {
        return database.cookieQueries.getSortedCookies().asFlow().mapToList(Dispatchers.IO).first().firstOrNull()?.cookie
    }
}
