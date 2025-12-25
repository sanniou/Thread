package ai.saniou.thread.data.mapper

import ai.saniou.thread.db.table.Cookie
import kotlin.time.Instant
import ai.saniou.thread.domain.model.forum.Cookie as DomainCookie

fun Cookie.toDomain(): DomainCookie {
    return DomainCookie(
        alias = alias,
        value = cookie,
        sort = sort,
        lastUsedAt = Instant.fromEpochMilliseconds(lastUsedAt),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
    )
}

fun List<Cookie>.toDomain(): List<DomainCookie> {
    return this.map { it.toDomain() }
}
