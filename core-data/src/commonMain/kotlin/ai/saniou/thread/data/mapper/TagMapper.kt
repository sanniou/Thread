package ai.saniou.thread.data.mapper

import ai.saniou.thread.domain.model.bookmark.Tag

fun ai.saniou.thread.db.table.Tag.toDomain(): Tag {
    return Tag(
        id = id,
        name = name
    )
}

fun Tag.toEntity(): ai.saniou.thread.db.table.Tag {
    return ai.saniou.thread.db.table.Tag(
        id = id,
        name = name
    )
}
