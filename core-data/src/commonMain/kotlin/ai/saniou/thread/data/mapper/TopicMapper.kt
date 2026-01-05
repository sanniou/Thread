package ai.saniou.thread.data.mapper


import ai.saniou.thread.db.table.forum.CommentQueries
import ai.saniou.thread.db.table.forum.GetTopic
import ai.saniou.thread.db.table.forum.GetTopicsInChannelOffset
import ai.saniou.thread.db.table.forum.ImageQueries
import ai.saniou.thread.db.table.forum.SearchTopics
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.ImageType
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.TopicMetadata
import kotlin.time.Instant
import ai.saniou.thread.db.table.forum.Topic as EntityTopic


fun SearchTopics.toDomain(
    query: CommentQueries? = null,
    imageQueries: ImageQueries? = null,
): Topic {
    val author = Author(
        id = userHash,
        name = authorName,
        sourceName = sourceId
    )

    val images = imageQueries?.getImagesByParent(
        sourceId = sourceId,
        parentId = id,
        parentType = ImageType.Topic
    )?.executeAsList()?.map { it.toDomain() }
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
        images = images,
        isSage = sage > 0,
        isAdmin = admin > 0,
        isHidden = hide > 0,
        isLocal = false,
        lastViewedCommentId = lastViewedCommentId,
        comments = query?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toDomain()
        } ?: emptyList(),
        remainingCount = remainingCount
    )
}

fun GetTopicsInChannelOffset.toDomain(
    query: CommentQueries? = null,
    imageQueries: ImageQueries? = null,
): Topic {
    val author = Author(
        id = userHash,
        name = authorName,
        sourceName = sourceId
    )

    val images = imageQueries?.getImagesByParent(
        sourceId = sourceId,
        parentId = id,
        parentType = ImageType.Topic
    )?.executeAsList()?.map { it.toDomain() }
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
        images = images,
        isSage = sage > 0,
        isAdmin = admin > 0,
        isHidden = hide > 0,
        isLocal = false,
        lastViewedCommentId = lastViewedCommentId,
        comments = query?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toDomain()
        } ?: emptyList(),
        remainingCount = remainingCount
    )
}

fun GetTopic.toDomain(
    query: CommentQueries? = null,
    imageQueries: ImageQueries? = null,
): Topic {
    val author = Author(
        id = userHash,
        name = authorName,
        sourceName = sourceId
    )

    val images = imageQueries?.getImagesByParent(
        sourceId = sourceId,
        parentId = id,
        parentType = ImageType.Topic
    )?.executeAsList()?.map { it.toDomain() }
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
        images = images,
        isSage = sage > 0,
        isAdmin = admin > 0,
        isHidden = hide > 0,
        isLocal = false,
        lastViewedCommentId = lastViewedCommentId,
        comments = query?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toDomain()
        } ?: emptyList(),
        remainingCount = null
    )
}

fun EntityTopic.toDomain(
    query: CommentQueries? = null,
    imageQueries: ImageQueries? = null,
): Topic {
    val author = Author(
        id = userHash,
        name = authorName,
        sourceName = sourceId
    )

    val images = imageQueries?.getImagesByParent(
        sourceId = sourceId,
        parentId = id,
        parentType = ImageType.Topic
    )?.executeAsList()?.map { it.toDomain() }
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
        images = images,
        isSage = sage > 0,
        isAdmin = admin > 0,
        isHidden = hide > 0,
        isLocal = false,
        lastViewedCommentId = null,
        comments = query?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toDomain()
        } ?: emptyList(),
        remainingCount = null
    )
}

fun EntityTopic.toMetadata(
    query: CommentQueries? = null,
    imageQueries: ImageQueries? = null,
): TopicMetadata {
    val author = Author(
        id = userHash,
        name = authorName,
        sourceName = sourceId
    )

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
        isSage = sage > 0,
        isAdmin = admin > 0,
        isHidden = hide > 0,
        lastViewedCommentId = null,
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
        isSage = isSage,
        isAdmin = isAdmin,
        isHidden = isHidden,
        sourceName = sourceId,
        sourceUrl = sourceUrl,
        lastViewedCommentId = lastViewedCommentId
    )
}

fun Topic.toEntity(page: Int = 1): EntityTopic {
    return EntityTopic(
        id = id,
        sourceId = sourceId,
        channelId = channelId,
        commentCount = commentCount,
        createdAt = createdAt.toEpochMilliseconds(),
        userHash = author.id,
        authorName = author.name,
        title = title,
        content = content,
        summary = summary,
        sage = if (isSage) 1 else 0,
        admin = if (isAdmin) 1 else 0,
        hide = if (isHidden) 1 else 0,
        page = page.toLong()
    )
}
