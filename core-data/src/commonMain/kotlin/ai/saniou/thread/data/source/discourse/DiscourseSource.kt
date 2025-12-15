package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseCategory
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopic
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseUser
import ai.saniou.thread.domain.model.forum.Forum
import ai.saniou.thread.domain.model.forum.Post
import kotlinx.datetime.Instant
import ai.saniou.thread.domain.repository.Source

class DiscourseSource(
    private val api: DiscourseApi,
) : Source {
    override val id: String = "discourse"

    override suspend fun getForums(): Result<List<Forum>> {
        return try {
            val response = api.getCategories()
            val forums = response.categoryList.categories.map { it.toForum() }
            Result.success(forums)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPosts(forumId: String, page: Int): Result<List<Post>> {
        return try {
            val response = if (forumId == "latest") {
                api.getLatestTopics(page)
            } else {
                api.getCategoryTopics(forumId, page)
            }
            val usersMap = response.users.associateBy { it.id }

            val posts = response.topicList.topics.map { topic ->
                topic.toPost(usersMap)
            }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun DiscourseCategory.toForum(): Forum {
    return Forum(
        id = id.toString(),
        name = name,
        showName = name,
        msg = descriptionText ?: description ?: "",
        groupId = parentCategoryId?.toString() ?: "0",
        groupName = "Discourse", // Could map parent category name if we fetch it
        sourceName = "discourse",
        tag = null,
        threadCount = topicCount.toLong(),
        autoDelete = null,
        interval = null,
        safeMode = if (readRestricted) "restricted" else "public"
    )
}

private fun DiscourseTopic.toPost(usersMap: Map<Long, DiscourseUser>): Post {
    // 假设 topic 的第一个 poster 是楼主
    val originalPosterId =
        posters.firstOrNull { it.description.contains("Original Poster") }?.userId
            ?: posters.firstOrNull()?.userId
    val user = originalPosterId?.let { usersMap[it] }

    // 构造 Post 对象，这里需要根据实际 Post 结构进行适配
    // 注意：Discourse 的 Topic 和 NMB 的 Post 结构不同，这里做简化映射
    val postCreatedAt = try {
        Instant.parse(createdAt)
    } catch (e: Exception) {
        Instant.fromEpochMilliseconds(0)
    }

    return Post(
        id = id.toString(),
        fid = categoryId,
        replyCount = replyCount.toLong(),
        img = imageUrl ?: "",
        ext = "", // Discourse 通常没有扩展名概念，图片直接是 URL
        now = createdAt, // 使用创建时间
        userHash = user?.username ?: "Unknown",
        name = user?.name ?: user?.username ?: "Anonymous",
        title = title,
        content = fancyTitle, // 暂时用 fancyTitle 作为 content，实际可能需要 fetch 帖子详情
        sage = 0, // Discourse 没有 sage
        admin = if (pinned || closed) 1 else 0,
        hide = if (!visible) 1 else 0,
        createdAt = postCreatedAt,
        sourceName = "discourse",
        sourceUrl = "https://meta.discourse.org/t/$slug/$id",
        author = user?.name ?: user?.username ?: "Anonymous",
        forumName = "Discourse", // Could be mapped from category if available
        isSage = false,
        isAdmin = pinned || closed,
        isHidden = !visible,
        isLocal = false,
        lastReadReplyId = 0,
        replies = null,
        remainReplies = null,
    )
}
