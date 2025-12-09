package ai.saniou.thread.data.repository

import ai.saniou.nmb.db.table.Cookie
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.thread.domain.repository.UserRepository
import ai.saniou.thread.domain.model.Cookie as DomainCookie

class UserRepositoryImpl(
    private val nmbSource: NmbSource,
) : UserRepository {
    override suspend fun getCookiesList(): List<DomainCookie> {
        return nmbSource.getSortedCookies().toDomain()
    }

    override suspend fun addCookie(name: String, value: String) {
        nmbSource.insertCookie(
            alias = name,
            cookie = value,
        )
    }

    override suspend fun deleteCookie(cookie: DomainCookie) {
        nmbSource.deleteCookie(cookie.value)
    }

    override suspend fun updateCookieSort(cookies: List<DomainCookie>) {
        nmbSource.updateCookiesSort(
            cookies.map {
                Cookie(
                    cookie = it.value,
                    alias = it.alias,
                    sort = it.sort,
                    createdAt = it.createdAt,
                    lastUsedAt = it.lastUsedAt
                )
            }
        )
    }
}
