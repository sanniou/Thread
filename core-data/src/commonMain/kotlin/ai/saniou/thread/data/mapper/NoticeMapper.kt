package ai.saniou.thread.data.mapper

import ai.saniou.thread.db.table.Notice
import kotlin.time.Instant
import ai.saniou.thread.domain.model.forum.Notice as DomainNotice

fun Notice.toDomain(): DomainNotice {
    return DomainNotice(
        id = id,
        content = content,
        date = Instant.fromEpochMilliseconds(date),
        enable = enable == 1L,
        isRead = readed == 1L
    )
}
