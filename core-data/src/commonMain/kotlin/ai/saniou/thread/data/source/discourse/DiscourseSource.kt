package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseCategory
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopic
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseUser
import ai.saniou.thread.domain.model.forum.Forum
import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.model.forum.ThreadReply
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
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

    override fun getThreadsPager(
        forumId: String,
        isTimeline: Boolean,
        initialPage: Int,
    ): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = 30),
            initialKey = initialPage,
            pagingSourceFactory = { DiscoursePagingSource(api, forumId) }
        ).flow
    }

    override suspend fun getThreadDetail(threadId: String, page: Int): Result<Post> {
        return try {
            val response = api.getTopic(threadId, page)
            // Need to map detail response to Post
            // Since toPost extension is for DiscourseTopic (list item), we might need another mapper or adapt.
            // For now, let's create a basic Post from detail.
            val firstPost = response.postStream.posts.firstOrNull()
            val createdAt = try {
                 Instant.parse(firstPost?.createdAt ?: "")
            } catch (e: Exception) {
                 Instant.fromEpochMilliseconds(0)
            }
            val post = Post(
                id = response.id.toString(),
                fid = response.categoryId,
                replyCount = response.replyCount.toLong(),
                img = "", // Detail doesn't easily give single image unless parsed from content
                ext = "",
                now = firstPost?.createdAt ?: "",
                userHash = firstPost?.username ?: "",
                name = firstPost?.name ?: firstPost?.username ?: "Anonymous",
                title = response.title,
                content = firstPost?.cooked ?: "", // Use full content
                sage = 0,
                admin = 0,
                hide = 0,
                createdAt = createdAt,
                sourceName = "discourse",
                sourceUrl = "https://meta.discourse.org/t/${response.id}",
                author = firstPost?.name ?: firstPost?.username ?: "Anonymous",
                forumName = "Discourse",
                isSage = false,
                isAdmin = false,
                isHidden = false,
                isLocal = false,
                lastReadReplyId = 0,
                replies = null,
                remainReplies = null
            )
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getThreadRepliesPager(
        threadId: String,
        initialPage: Int
    ): Flow<PagingData<ThreadReply>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            initialKey = initialPage,
            pagingSourceFactory = { DiscourseThreadPagingSource(api, threadId) }
        ).flow
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

internal fun DiscourseTopic.toPost(usersMap: Map<Long, DiscourseUser>): Post {
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
        content = excerpt ?: fancyTitle,
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
