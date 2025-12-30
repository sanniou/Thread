package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.cache.CacheStrategy
import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.mapper.flatten
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toDomainTree
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopic
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseUser
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.observeValue
import ai.saniou.thread.network.SaniouResult
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Topic

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

    override fun observeChannels(): Flow<List<Channel>> {
        return cache.observeForums(id).map { forums ->
            if (forums.isNotEmpty()) {
                buildChannelTree(forums.map { it.toDomain() })
            } else {
                emptyList()
            }
        }
    }

    override suspend fun fetchChannels(): Result<Unit> {
        val needUpdate = CacheStrategy.shouldFetch(
            db = db,
            keyType = RemoteKeyType.FORUM_CATEGORY,
            keyId = "${id}_forums",
            expiration = 1.days
        )

        if (!needUpdate) {
            return Result.success(Unit)
        }

        return try {
            // 1. Try to fetch from network
            when (val response = api.getCategories()) {
                is SaniouResult.Success -> {
                    val forums = response.data.categoryList.categories.map { it.toDomainTree(id) }

                    // 2. Save to cache (flatten first)
                    val flatForums = forums.flatMap { it.flatten() }
                    cache.saveForums(flatForums.map { it.toEntity() })

                    CacheStrategy.updateLastFetchTime(
                        db = db,
                        keyType = RemoteKeyType.FORUM_CATEGORY,
                        keyId = "${id}_forums"
                    )

                    Result.success(Unit)
                }
                is SaniouResult.Error -> {
                    Result.failure(response.ex)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildChannelTree(flatChannels: List<Channel>): List<Channel> {
        val channelMap = flatChannels.associateBy { it.id }
        val rootChannels = mutableListOf<Channel>()

        // First pass: identify roots and prepare children lists
        val childrenMap = mutableMapOf<String, MutableList<Channel>>()

        flatChannels.forEach { channel ->
            val parentId = channel.parentId
            if (parentId == null || !channelMap.containsKey(parentId)) {
                rootChannels.add(channel)
            } else {
                childrenMap.getOrPut(parentId) { mutableListOf() }.add(channel)
            }
        }

        // Recursive function to build tree
        fun buildTree(channel: Channel): Channel {
            val children = childrenMap[channel.id]?.map { buildTree(it) }?.sortedBy { it.sort } ?: emptyList()
            return channel.copy(children = children)
        }

        return rootChannels.map {
            // Discourse 没有 category ，这里硬编码
            buildTree(it.copy(groupName="Discourse"))
        }.sortedBy { it.sort }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getThreadsPager(
        forumId: String,
        isTimeline: Boolean,
        initialPage: Int,
    ): Flow<PagingData<Topic>> {
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
            pagingData.map { it.toDomain(db.commentQueries, db.imageQueries) }
        }
    }

    override suspend fun getThreadDetail(threadId: String, page: Int): Result<Topic> {
        return when (val result = api.getTopic(threadId, page)) {
            is SaniouResult.Success -> {
                try {
                    val response = result.data
                    val firstPost = response.postStream.posts.firstOrNull()
                    val createdAt = try {
                        Instant.parse(firstPost?.createdAt ?: "")
                    } catch (e: Exception) {
                        Instant.fromEpochMilliseconds(0)
                    }
                    val post = Topic(
                        id = response.id.toString(),
                        channelId = response.categoryId.toString(), // fid -> channelId
                        commentCount = response.replyCount.toLong(), // replyCount -> commentCount
                        images = emptyList(), // TODO: parse images
                        createdAt = kotlin.time.Instant.fromEpochMilliseconds(createdAt.toEpochMilliseconds()),
                        title = response.title,
                        content = firstPost?.cooked ?: "", // Use full content
                        sourceName = "discourse",
                        sourceUrl = "https://meta.discourse.org/t/${response.id}",
                        author = Author(
                            id = firstPost?.username ?: "",
                            name = firstPost?.name ?: firstPost?.username ?: "Anonymous"
                        ), // author -> Author object
                        channelName = "Discourse", // forumName -> channelName
                        isSage = false,
                        isAdmin = false,
                        isHidden = false,
                        isLocal = false,
                        lastViewedCommentId = "",
                        comments = emptyList(),
                        remainingCount = null
                    )

                    // Update cache/DB
                    // TODO: Implement proper caching for thread details similar to NmbSource

                    Result.success(post)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            is SaniouResult.Error -> {
                Result.failure(result.ex)
            }
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getThreadRepliesPager(
        threadId: String,
        initialPage: Int,
        isPoOnly: Boolean,
    ): Flow<PagingData<Comment>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            initialKey = initialPage,
            remoteMediator = DiscourseThreadRemoteMediator(
                sourceId = id,
                topicId = threadId,
                db = db,
                dataPolicy = DataPolicy.NETWORK_ELSE_CACHE,
                initialPage = initialPage,
                fetcher = { page -> api.getTopic(threadId, page) }
            ),
            pagingSourceFactory = {
                cache.getThreadRepliesPagingSource(id, threadId)
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain(db.imageQueries) }
        }
    }

    override fun getForum(forumId: String): Flow<Channel?> {
        // TODO: Implement proper forum detail fetching or caching
        // For now, return null as we don't persist forums locally yet
        return kotlinx.coroutines.flow.flowOf(null)
    }
}

internal fun DiscourseTopic.toPost(usersMap: Map<Long, DiscourseUser>): Topic {
// 假设 topic 的第一个 poster 是楼主
val originalPosterId =
    posters?.firstOrNull { it.description.contains("Original Poster") }?.userId
        ?: posters?.firstOrNull()?.userId
val user = originalPosterId?.let { usersMap[it] }

val postCreatedAt = try {
    Instant.parse(createdAt)
} catch (e: Exception) {
    Instant.fromEpochMilliseconds(0)
}

return Topic(
    id = id.toString(),
    channelId = categoryId?.toString() ?: "0", // fid -> channelId
    commentCount = replyCount.toLong(), // replyCount -> commentCount
    images = imageUrl?.let {
        listOf(ai.saniou.thread.domain.model.forum.Image(originalUrl = it, thumbnailUrl = it))
    } ?: emptyList(), // img/ext -> images
    title = title,
    content = excerpt ?: fancyTitle,
    createdAt = kotlin.time.Instant.fromEpochMilliseconds(postCreatedAt.toEpochMilliseconds()), // now -> createdAt
    sourceName = "discourse",
    sourceUrl = "https://meta.discourse.org/t/$slug/$id",
    author = Author(
        id = user?.username ?: "Unknown",
        name = user?.name ?: user?.username ?: "Anonymous"
    ),
    channelName = "Discourse", // forumName -> channelName
    isSage = false,
    isAdmin = pinned || closed,
    isHidden = !visible,
    isLocal = false,
    lastViewedCommentId = "",
    comments = emptyList(),
    remainingCount = null,
)
}
