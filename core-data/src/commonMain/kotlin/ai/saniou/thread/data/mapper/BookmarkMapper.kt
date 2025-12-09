package ai.saniou.thread.data.mapper

import ai.saniou.nmb.db.table.Bookmark as BookmarkEntity
import ai.saniou.thread.domain.model.Bookmark

/**
 * 将数据库实体 BookmarkEntity 转换为领域模型 Bookmark。
 */
fun BookmarkEntity.toDomain(): Bookmark {
    return Bookmark(
        postId = this.postId,
        content = this.content,
        tag = this.tag,
        createdAt = this.createdAt
    )
}