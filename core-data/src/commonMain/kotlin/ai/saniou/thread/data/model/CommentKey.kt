package ai.saniou.thread.data.model

import kotlinx.serialization.Serializable

/**
 * 评论列表的分页键
 *
 * @param floor 楼层 (主要排序键)
 * @param id 评论 ID (次要排序键，用于 Tie-breaker)
 */
@Serializable
data class CommentKey(
    val floor: Long,
    val id: String
) {
    override fun toString(): String = "$floor:$id"

    companion object {
        fun fromString(str: String): CommentKey {
            val parts = str.split(":")
            return CommentKey(
                floor = parts[0].toLong(),
                id = parts[1]
            )
        }
    }
}
