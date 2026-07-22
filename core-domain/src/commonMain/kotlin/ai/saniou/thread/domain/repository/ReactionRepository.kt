package ai.saniou.thread.domain.repository

interface ReactionRepository {
    suspend fun upvoteTopic(sourceId: String, topicId: String): Result<Unit>
    suspend fun downvoteTopic(sourceId: String, topicId: String): Result<Unit>

    /**
     * 推荐/信息流「不感兴趣」。[channelId] 可选（贴吧 fid）。
     */
    suspend fun submitNotInterested(
        sourceId: String,
        topicId: String,
        channelId: String? = null,
        reasonIds: String = "",
        extra: String = "",
        clickTimeMs: Long = 0L,
    ): Result<String>
}
