package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.db.Database
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

class NmbCookieProvider(
    private val database: Database
) {
    suspend fun getCookieValue(): String? {
        val cookie = database.cookieQueries.getSortedCookies().asFlow().mapToList(Dispatchers.Default).first().firstOrNull()?.cookie
        return cookie?.let { "userhash=$it" }
    }
}
