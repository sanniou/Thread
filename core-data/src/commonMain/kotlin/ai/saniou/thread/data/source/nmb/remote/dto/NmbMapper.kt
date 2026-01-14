package ai.saniou.thread.data.source.nmb.remote.dto

import ai.saniou.corecommon.utils.toTime
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.db.table.forum.ImageQueries
import ai.saniou.thread.db.table.forum.SelectSubscriptionTopic
import ai.saniou.thread.db.table.forum.TimeLine
import ai.saniou.thread.domain.model.Tag
import ai.saniou.thread.domain.model.TagType
import ai.saniou.thread.domain.model.forum.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun TimeLine.toDomain(): Channel = Channel(
    id = id.toString(),
    name = name,
    sourceName = "nmb",
    tag = "timeline",
    displayName = displayName,
    description = notice,
    descriptionText = null,
    groupId = "-1",
    groupName = "TimeLine",
    topicCount = maxPage * 20, // Approximate thread count
    postCount = null,
    autoDelete = null,
    interval = null,
    safeMode = null,
    parentId = null,
    color = null,
    textColor = null,
    icon = null,
    emoji = null,
    styleType = null,
    listViewStyle = null,
    logoUrl = null,
    bannerUrl = null,
    slug = null,
    canCreateTopic = null,
    sort = null
)

// Helper to create single Image list
private fun createImageList(img: String?, ext: String?, cdnUrl: String): List<Image> {
    return if (!img.isNullOrEmpty() && !ext.isNullOrEmpty()) {
        val baseUrl = cdnUrl.removeSuffix("/")
        listOf(
            Image(
                originalUrl = "$baseUrl/image/$img$ext",
                thumbnailUrl = "$baseUrl/thumb/$img$ext",
                extension = ext
            )
        )
    } else {
        emptyList()
    }
}

// Helper to create Author
private fun createAuthor(userHash: String?, name: String?): Author {
    return Author(
        id = userHash ?: "",
        name = name ?: "",
        sourceName = "nmb"
    )
}

@OptIn(ExperimentalTime::class)
fun SelectSubscriptionTopic.toDomain(imageQueries: ImageQueries? = null): Topic {
    val images = imageQueries?.getImagesByParent(
        sourceId = "nmb",
        parentId = id,
        parentType = ImageType.Topic
    )?.executeAsList()?.map { it.toDomain() }
        ?: emptyList()

    return Topic(
        id = id,
        sourceName = "nmb",
        sourceId = "nmb",
        sourceUrl = "https://nmb.ai/thread/$id",
        title = title ?: "",
        content = content ?: "",
        author = createAuthor(authorId, authorName),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        channelId = channelId,
        channelName = "", // DB doesn't store
        commentCount = commentCount ?: 0,
        images = images,
        isLocal = isLocal == 1L,
        comments = emptyList(),
        tags = emptyList(), // TODO: Map tags from DB if available
        summary = null,
        lastViewedCommentId = null
    )
}

@OptIn(ExperimentalTime::class)
fun Thread.toDomain(cdnUrl: String): Topic = Topic(
    id = id.toString(),
    sourceName = "nmb",
    sourceId = "nmb",
    sourceUrl = "https://nmb.ai/thread/$id",
    title = title,
    content = content ?: "",
    author = createAuthor(userHash, name),
    createdAt = now.toTime(), // DTO uses 'now' string
    channelId = fid.toString(),
    channelName = "",
    commentCount = replyCount,
    images = createImageList(img, ext, cdnUrl),
    isLocal = false,
    comments = replies.filter { it.id != 9999999L }.map { it.toDomain(id.toString(), cdnUrl) },
    summary = null,
    tags = mutableListOf<Tag>().apply {
        if (sage > 0) add(Tag(id = "sage", name = "SAGE", type = TagType.SYSTEM))
        if (admin > 0) add(Tag(id = "admin", name = "Admin", type = TagType.SYSTEM))
        if (hide > 0) add(Tag(id = "hide", name = "Hide", type = TagType.SYSTEM))
    },
)

@OptIn(ExperimentalTime::class)
fun ThreadReply.toDomain(topicId: String, cdnUrl: String): Comment {
    return Comment(
        id = id.toString(),
        topicId = topicId,
        author = createAuthor(userHash, name),
        createdAt = now.toTime(),
        title = title,
        content = content,
        images = createImageList(img, ext, cdnUrl),
        isAdmin = admin > 0,
        floor = 1, // API might not provide floor todo get by page
        replyToId = null,
        sourceId = "nmb",
    )
}

@OptIn(ExperimentalTime::class)
fun Thread.toDomainComment(sourceId: String, cdnUrl: String): Comment {
    return Comment(
        id = id.toString(),
        topicId = id.toString(), // 主楼的 topicId 就是它自己
        author = createAuthor(userHash, name),
        createdAt = now.toTime(),
        title = title,
        content = content,
        images = createImageList(img, ext, cdnUrl),
        isAdmin = admin > 0,
        floor = 1, // 主楼默认 1 楼
        replyToId = null,
        sourceId = sourceId
    )
}


@OptIn(ExperimentalTime::class)
fun ForumThread.toDomain(cdnUrl: String): Topic {
    val author = Author(
        id = userHash,
        name = name,
        sourceName = "nmb"
    )
    val images = createImageList(img, ext, cdnUrl)

    return Topic(
        id = id.toString(),
        sourceName = "nmb",
        sourceId = "nmb",
        sourceUrl = "https://nmb.ai/thread/$id",
        title = title,
        content = content,
        summary = content,
        author = author,
        createdAt = now.toTime(),
        channelId = fid.toString(), // channelId
        channelName = "",
        commentCount = replyCount, // commentCount
        images = images,
        isLocal = false,
        // fixme  后续处理 lastReadCommentId 和 comments
        lastViewedCommentId = null,
        tags = mutableListOf<Tag>().apply {
            if (sage > 0) add(Tag(id = "sage", name = "SAGE", type = TagType.SYSTEM))
            if (admin > 0) add(Tag(id = "admin", name = "Admin", type = TagType.SYSTEM))
            if (hide > 0) add(Tag(id = "hide", name = "Hide", type = TagType.SYSTEM))
        },
        orderKey = (replies.maxOfOrNull { it.now.nowToEpochMilliseconds() }
            ?: now.nowToEpochMilliseconds()),
        comments = replies.map { it.toDomain(id.toString(), cdnUrl) },
    )
}
