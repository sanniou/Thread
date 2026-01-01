package ai.saniou.thread.data.mapper

import ai.saniou.thread.db.table.Account as Cookie
import kotlin.time.Instant
import ai.saniou.thread.domain.model.forum.Account

fun Cookie.toDomain(): Account {
    return Account(
        id = id,
        sourceId = source_id,
        alias = alias,
        value = account,
        uid = uid,
        avatar = avatar,
        extraData = extra_data,
        sort = sort,
        isCurrent = is_current == 1L,
        lastUsedAt = Instant.fromEpochMilliseconds(lastUsedAt),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
    )
}

fun List<Cookie>.toDomain(): List<Account> {
    return this.map { it.toDomain() }
}
