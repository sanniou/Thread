package ai.saniou.thread.data.mapper


import ai.saniou.thread.db.table.TopicTagQueries
import ai.saniou.thread.db.table.forum.CommentQueries
import ai.saniou.thread.db.table.forum.GetTopic
import ai.saniou.thread.db.table.forum.GetTopicsInChannelOffset
import ai.saniou.thread.db.table.forum.ImageQueries
import ai.saniou.thread.db.table.forum.SearchTopics
import ai.saniou.thread.domain.model.SourceCapabilities
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.ImageType
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.TopicMetadata
import kotlin.time.Instant
import ai.saniou.thread.db.table.forum.Topic as EntityTopic


fun SearchTopics.toDomain(
    query: CommentQueries? = null,
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
        sourceUrl = "https://nmb.ai/thread/$id", // TODO: Move URL generation to Source logic
        title = title,
        content = content ?: summary ?: "", // If content is null, fallback to summary, then empty
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
        comments = query?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toDomain()
        } ?: emptyList(),
        remainingCount = remainingCount,
        tags = tags
    )
}

fun GetTopicsInChannelOffset.toDomain(
    query: CommentQueries? = null,
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
        sourceUrl = "https://nmb.ai/thread/$id", // TODO: Move URL generation to Source logic
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
        comments = query?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toDomain()
        } ?: emptyList(),
        remainingCount = remainingCount,
        tags = tags
    )
}

fun GetTopic.toDomain(
    query: CommentQueries? = null,
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
        sourceUrl = "https://nmb.ai/thread/$id", // TODO: Move URL generation to Source logic
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
        comments = query?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toDomain()
        } ?: emptyList(),
        remainingCount = null,
        tags = tags
    )
}

fun EntityTopic.toDomain(
    query: CommentQueries? = null,
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
        sourceUrl = "https://nmb.ai/thread/$id", // TODO: Move URL generation to Source logic
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
        lastViewedCommentId = null,
        comments = query?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toDomain()
        } ?: emptyList(),
        remainingCount = null,
        tags = tags
    )
}

fun EntityTopic.toMetadata(
    query: CommentQueries? = null,
    imageQueries: ImageQueries? = null,
    topicTagQueries: TopicTagQueries? = null,
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
        sourceUrl = "https://nmb.ai/thread/$id", // TODO: Move URL generation to Source logic
        title = title,
        author = author,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        channelId = channelId,
        channelName = "", // DB Topic table usually doesn't store channel name, need Join or lookup
        commentCount = commentCount,
        tags = tags,
        lastViewedCommentId = null,
        totalPages = when (sourceId) {
            "nmb" -> (commentCount / 19).toInt() + if (commentCount % 19 > 0) 1 else 0
            "tieba" -> (commentCount / 30).toInt() + if (commentCount % 30 > 0) 1 else 0
            else -> null
        },
        agreeCount = agreeCount,
        disagreeCount = disagreeCount,
        isCollected = isCollected,
        capabilities = when (sourceId) {
            "nmb" -> SourceCapabilities.Nmb
            "tieba" -> SourceCapabilities.Tieba
            else -> SourceCapabilities.Default
        }
    )
}

fun Topic.toMetadata(): TopicMetadata {
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
        totalPages = when (sourceId) {
            "nmb" -> (commentCount / 19).toInt() + if (commentCount % 19 > 0) 1 else 0
            "tieba" -> (commentCount / 30).toInt() + if (commentCount % 30 > 0) 1 else 0
            else -> null
        },
        agreeCount = agreeCount,
        disagreeCount = disagreeCount,
        isCollected = isCollected,
        capabilities = when (sourceId) {
            "nmb" -> SourceCapabilities.Nmb
            "tieba" -> SourceCapabilities.Tieba
            else -> SourceCapabilities.Default
        }
    )
}

fun Topic.toEntity(page: Int = 1): EntityTopic {
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
        page = page.toLong(),
        agreeCount = agreeCount,
        disagreeCount = disagreeCount,
        isCollected = isCollected
    )
}
