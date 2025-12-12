package ai.saniou.thread.data.mapper

import ai.saniou.thread.db.table.Notice
import ai.saniou.thread.domain.model.forum.Notice as DomainNotice

fun Notice.toDomain(): DomainNotice {
    return DomainNotice(
        id = id,
        content = content,
        date = date,
        enable = enable == 1L,
        isRead = readed == 1L
    )
}
