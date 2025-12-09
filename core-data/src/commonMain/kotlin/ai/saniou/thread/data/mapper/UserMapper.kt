package ai.saniou.thread.data.mapper

import ai.saniou.nmb.db.table.Cookie
import ai.saniou.thread.domain.model.Cookie as DomainCookie

fun Cookie.toDomain(): DomainCookie {
    return DomainCookie(
        alias = alias,
        value = cookie,
        sort = sort,
        lastUsedAt = lastUsedAt,
        createdAt = createdAt,
    )
}

fun List<Cookie>.toDomain(): List<DomainCookie> {
    return this.map { it.toDomain() }
}
