package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseCategory
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopic
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseUser
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.domain.model.forum.Forum
import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.model.forum.ThreadReply
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.PagingData
import app.cash.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.data.source.nmb.remote.dto.toThreadWithInformation
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.GetThreadsInForumOffset
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.observeValue

class DiscourseSource(
    private val api: DiscourseApi,
    private val cache: SourceCache,
    private val db: Database,
    private val settingsRepository: SettingsRepository,
) : Source {
    override val id: String = "discourse"
    override val name: String = "Discourse"

    override val isInitialized: Flow<Boolean> =
        settingsRepository.observeValue<Boolean>("discourse_initialized")
            .map { it == true }

    override suspend fun getForums(): Result<List<Forum>> {
        return try {
            // 1. Try to fetch from network
            val response = api.getCategories()
            val forums = response.categoryList.categories.map { it.toForum() }

            // 2. Save to cache
            cache.saveForums(forums.map { it.toEntity() })

            Result.success(forums)
        } catch (e: Exception) {
            // 3. Fallback to cache if network fails
            try {
                val cachedForums = cache.getForums(id).map { it.toDomain() }
                if (cachedForums.isNotEmpty()) {
                    Result.success(cachedForums)
                } else {
                    Result.failure(e)
                }
            } catch (cacheError: Exception) {
                // If cache also fails, return original error
                Result.failure(e)
            }
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

    @OptIn(ExperimentalPagingApi::class)
    override fun getThreadsPager(
        forumId: String,
        isTimeline: Boolean,
        initialPage: Int,
    ): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = 30),
            initialKey = initialPage,
            remoteMediator = DiscourseRemoteMediator(
                sourceId = id,
                fid = forumId,
                api = api,
                cache = cache,
                initialPage = initialPage,
                dataPolicy = DataPolicy.NETWORK_ELSE_CACHE,
                db = db,
            ),
            pagingSourceFactory = {
                cache.getForumThreadsPagingSource(id, forumId)
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toThreadWithInformation().toDomain() }
        }
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
                lastReadReplyId = "",
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
        initialPage: Int,
    ): Flow<PagingData<ThreadReply>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            initialKey = initialPage,
            pagingSourceFactory = { DiscourseThreadPagingSource(api, threadId) }
        ).flow
    }

    override fun getForum(forumId: String): Flow<Forum?> {
        // TODO: Implement proper forum detail fetching or caching
        // For now, return null as we don't persist forums locally yet
        return kotlinx.coroutines.flow.flowOf(null)
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
        lastReadReplyId = "",
        replies = null,
        remainReplies = null,
    )
}
