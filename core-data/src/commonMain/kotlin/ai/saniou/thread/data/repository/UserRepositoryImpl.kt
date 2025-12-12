package ai.saniou.thread.data.repository

import ai.saniou.nmb.db.Database
import ai.saniou.nmb.db.table.Cookie
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.domain.repository.UserRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import ai.saniou.thread.domain.model.forum.Cookie as DomainCookie

@OptIn(ExperimentalTime::class)
class UserRepositoryImpl(
    private val db: Database,
) : UserRepository {
    override suspend fun getCookiesList(): List<DomainCookie> {
        return getSortedCookies().toDomain()
    }

    override suspend fun addCookie(name: String, value: String) {
        val now = Clock.System.now().epochSeconds
        val count =
            db.cookieQueries.countCookies().asFlow().mapToList(Dispatchers.Default).first().size
        db.cookieQueries.insertCookie(
            cookie = value,
            alias = name,
            sort = count.toLong(),
            createdAt = now,
            lastUsedAt = now
        )
    }

    override suspend fun deleteCookie(cookie: DomainCookie) {
        db.cookieQueries.deleteCookie(cookie.value)
    }

    override suspend fun updateCookieSort(cookies: List<DomainCookie>) {
        db.cookieQueries.transaction {
            cookies.forEachIndexed { index, cookie ->
                db.cookieQueries.updateCookieSort(index.toLong(), cookie.value)
            }
        }
    }

    private suspend fun getSortedCookies(): List<Cookie> {
        return db.cookieQueries.getSortedCookies().asFlow().mapToList(Dispatchers.Default).first()
    }

    suspend fun getCookieValue(): String? {
        return getSortedCookies().firstOrNull()?.cookie
    }
}
