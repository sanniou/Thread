package ai.saniou.thread.data.mapper

import ai.saniou.thread.domain.model.Tag

fun ai.saniou.nmb.db.table.Tag.toDomain(): Tag {
    return Tag(
        id = id,
        name = name
    )
}

fun Tag.toEntity(): ai.saniou.nmb.db.table.Tag {
    return ai.saniou.nmb.db.table.Tag(
        id = id,
        name = name
    )
}