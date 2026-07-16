package ai.saniou.thread.data.mapper


import ai.saniou.thread.db.table.TopicTagQueries
import ai.saniou.thread.db.table.forum.CommentQueries
import ai.saniou.thread.db.table.forum.GetTopicsInChannelKeyset
import ai.saniou.thread.db.table.forum.ImageQueries
import ai.saniou.thread.domain.model.SourceCapabilities
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.ImageType
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.TopicMetadata
import kotlin.time.Instant
import ai.saniou.thread.db.table.forum.Topic as EntityTopic


fun EntityTopic.toDomain(
    commentQueries: CommentQueries? = null,
    imageQueries: ImageQueries? = null,
    topicTagQueries: TopicTagQueries? = null,
): Topic {
    val author = Author(
        id = authorId,
        name = authorName,
        sourceName = sourceId
    )

    val images = imageQueries?.getImagesByParent(
        sourceId = sourceId,
        parentId = id,
        parentType = ImageType.Topic
    )?.executeAsList()?.map { it.toDomain() }
        ?: emptyList()

    val tags =
        topicTagQueries?.getTagsForTopic(sourceId, id)?.executeAsList()?.map { it.toDomain() }
            ?: emptyList()

    return Topic(
        id = id,
        sourceName = sourceId,
        sourceId = sourceId,
        sourceUrl = "",
        title = title,
        content = content ?: summary ?: "",
        summary = summary,
        author = author,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        channelId = channelId,
        channelName = "", // DB Topic table usually doesn't store channel name, need Join or lookup
        commentCount = commentCount,
        agreeCount = agreeCount,
        disagreeCount = disagreeCount,
        isCollected = isCollected,
        images = images,
        isLocal = false,
        lastViewedCommentId = lastViewedCommentId,
        comments = commentQueries?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toDomain(imageQueries, commentQueries)
        } ?: emptyList(),
        tags = tags,
        lastReplyAt = lastReplyAt,
    )
}

fun GetTopicsInChannelKeyset.toDomain(
    commentQueries: CommentQueries? = null,
    imageQueries: ImageQueries? = null,
    topicTagQueries: TopicTagQueries? = null,
): Topic {
    val author = Author(
        id = authorId,
        name = authorName,
        sourceName = sourceId
    )

    val images = imageQueries?.getImagesByParent(
        sourceId = sourceId,
        parentId = id,
        parentType = ImageType.Topic
    )?.executeAsList()?.map { it.toDomain() }
        ?: emptyList()

    val tags =
        topicTagQueries?.getTagsForTopic(sourceId, id)?.executeAsList()?.map { it.toDomain() }
            ?: emptyList()

    return Topic(
        id = id,
        sourceName = sourceId,
        sourceId = sourceId,
        sourceUrl = "",
        title = title,
        content = content ?: summary ?: "",
        summary = summary,
        author = author,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        channelId = channelId,
        channelName = "", // DB Topic table usually doesn't store channel name, need Join or lookup
        commentCount = commentCount,
        agreeCount = agreeCount,
        disagreeCount = disagreeCount,
        isCollected = isCollected,
        images = images,
        isLocal = false,
        lastViewedCommentId = lastViewedCommentId,
        comments = commentQueries?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toDomain(imageQueries, commentQueries)
        } ?: emptyList(),
        tags = tags,
        lastReplyAt = lastReplyAt,
    )
}

fun EntityTopic.toMetadata(
    query: CommentQueries? = null,
    imageQueries: ImageQueries? = null,
    topicTagQueries: TopicTagQueries? = null,
    capabilities: SourceCapabilities = SourceCapabilities.Default,
    sourceUrl: String = "",
): TopicMetadata {
    val author = Author(
        id = authorId,
        name = authorName,
        sourceName = sourceId
    )

    val tags =
        topicTagQueries?.getTagsForTopic(sourceId, id)?.executeAsList()?.map { it.toDomain() }
            ?: emptyList()

    return TopicMetadata(
        id = id,
        sourceName = sourceId,
        sourceUrl = sourceUrl,
        title = title,
        author = author,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        channelId = channelId,
        channelName = "", // DB Topic table usually doesn't store channel name, need Join or lookup
        commentCount = commentCount,
        tags = tags,
        lastViewedCommentId = lastViewedCommentId,
        totalPages = capabilities.commentPageSize?.let { size ->
            (commentCount / size).toInt() + if (commentCount % size > 0) 1 else 0
        },
        agreeCount = agreeCount,
        disagreeCount = disagreeCount,
        isCollected = isCollected,
        capabilities = capabilities,
    )
}

fun Topic.toMetadata(
    capabilities: SourceCapabilities = SourceCapabilities.Default,
    sourceUrl: String = this.sourceUrl,
): TopicMetadata {
    return TopicMetadata(
        id = id,
        channelId = channelId,
        channelName = channelName,
        title = title,
        author = author,
        createdAt = createdAt,
        commentCount = commentCount,
        tags = tags,
        sourceName = sourceId,
        sourceUrl = sourceUrl,
        lastViewedCommentId = lastViewedCommentId,
        totalPages = capabilities.commentPageSize?.let { size ->
            (commentCount / size).toInt() + if (commentCount % size > 0) 1 else 0
        },
        agreeCount = agreeCount,
        disagreeCount = disagreeCount,
        isCollected = isCollected,
        capabilities = capabilities,
    )
}

fun Topic.toEntityNoLastKey(
): EntityTopic {
    return EntityTopic(
        id = id,
        sourceId = sourceId,
        channelId = channelId,
        commentCount = commentCount,
        createdAt = createdAt.toEpochMilliseconds(),
        authorId = author.id,
        authorName = author.name,
        title = title,
        content = content,
        summary = summary,
        agreeCount = agreeCount,
        disagreeCount = disagreeCount,
        isCollected = isCollected,
        lastReplyAt = lastReplyAt ?: 0L,
        lastVisitedAt = 0L, // Default value, will be preserved by upsert logic
        lastViewedCommentId = null, // Default value, will be preserved by upsert logic
    )
}
