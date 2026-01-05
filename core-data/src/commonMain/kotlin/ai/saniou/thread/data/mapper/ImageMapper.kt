package ai.saniou.thread.data.mapper

import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.ImageType
import ai.saniou.thread.db.table.forum.Image as EntityImage

fun EntityImage.toDomain(): Image {
    return Image(
        originalUrl = originalUrl,
        thumbnailUrl = thumbnailUrl ?: originalUrl,
        name = name,
        extension = extension,
        width = width?.toInt(),
        height = height?.toInt()
        // path is not exposed to domain yet, but available in DB if needed
    )
}

fun Image.toEntity(
    sourcrId: String,
    parentId: String,
    parentType: ImageType,
    sortOrder: Long,
): EntityImage {
    return EntityImage(
        id = originalUrl,
        sourceId = sourcrId,
        parentId = parentId,
        parentType = parentType,
        originalUrl = originalUrl,
        thumbnailUrl = thumbnailUrl,
        name = name,
        extension = extension,
        path = null,
        width = width?.toLong(),
        height = height?.toLong(),
        sortOrder = sortOrder
    )
}
