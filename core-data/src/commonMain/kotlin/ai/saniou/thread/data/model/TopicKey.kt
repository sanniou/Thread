package ai.saniou.thread.data.model

import ai.saniou.thread.db.table.forum.Topic
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * 论坛帖子列表的分页键
 *
 * @param receiveDate 最后回复时间 (主要排序键)
 * @param topicId 帖子 ID (次要排序键，用于 Tie-breaker)
 */
@Serializable
data class TopicKey(
    val receiveDate: Long,
    val topicId: String,
    @Transient
    var topic: Topic? = null,
) {
    override fun toString(): String = "$receiveDate:$topicId"

    companion object {
        fun fromString(str: String): TopicKey {
            val parts = str.split(":")
            return TopicKey(
                receiveDate = parts[0].toLong(),
                topicId = parts[1]
            )
        }
    }
}

