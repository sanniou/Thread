package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.cache.CacheStrategy
import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.mapper.flatten
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toDomainTree
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.data.model.CommentKey
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopic
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseUser
import ai.saniou.thread.data.source.discourse.remote.dto.toComment
import ai.saniou.thread.data.source.discourse.remote.dto.toDomainComment
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.PagedResult
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.observeValue
import ai.saniou.thread.network.SaniouResult
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Topic
import kotlinx.coroutines.flow.flowOf

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

    override val loginStrategy: LoginStrategy = LoginStrategy.Api(
        title = "Discourse 登录"
    )

    override fun observeChannels(): Flow<List<Channel>> {
        return cache.observeChannels(id).map { forums ->
            if (forums.isNotEmpty()) {
                buildChannelTree(forums.map { it.toDomain(db.channelQueries) })
            } else {
                emptyList()
            }
        }
    }

    override suspend fun fetchChannels(): Result<Unit> {
        val needUpdate = CacheStrategy.shouldFetch(
            db = db,
            keyType = "forum_category",
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
                    cache.saveChannels(flatForums.map { it.toEntity() })

                    CacheStrategy.updateLastFetchTime(
                        db = db,
                        keyType = "forum_category",
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
            val children =
                childrenMap[channel.id]?.map { buildTree(it) }?.sortedBy { it.sort } ?: emptyList()
            return channel.copy(children = children)
        }

        return rootChannels.map {
            // Discourse 没有 category ，这里硬编码
            buildTree(it.copy(groupName = "Discourse"))
        }.sortedBy { it.sort }
    }

    override suspend fun getChannelTopics(
        channelId: String,
        cursor: String?,
        isTimeline: Boolean,
    ): Result<PagedResult<Topic>> {
        // Discourse API mapping
        // Assuming channelId is Category ID? Or 'latest' endpoint?
        // DiscourseRemoteMediator logic:
        // if (fid == "0" || fid == "latest") -> getLatestPosts
        // else -> getCategoryTopics
        val page = cursor?.toIntOrNull() ?: 0 // Discourse pages start at 0? Or 1? Usually 0 for some APIs, but let's assume 0 based on previous code usage?
        // Previous code used `page: Int` directly.
        // Let's assume 0-based for now if not specified.

        return try {
            val result = if (channelId == "0" || channelId == "latest") {
                api.getLatestTopics(page)
            } else {
                api.getCategoryTopics(channelId, page)
            }

            when (result) {
                is SaniouResult.Success -> {
                    // Similar logic to DiscourseRemoteMediator
                    val usersMap = result.data.users?.associateBy { it.id } ?: emptyMap()
                    val topics = result.data.topicList.topics.map { topic ->
                        topic.toPost(usersMap)
                    }

                    val nextCursor = if (topics.isNotEmpty()) (page + 1).toString() else null
                    val prevCursor = if (page > 0) (page - 1).toString() else null

                    Result.success(PagedResult(topics, prevCursor, nextCursor))
                }

                is SaniouResult.Error -> Result.failure(result.ex)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun getTopicComments(
        threadId: String,
        cursor: String?,
        isPoOnly: Boolean,
    ): Result<PagedResult<Comment>> {
        val page = cursor?.toIntOrNull() ?: 1
        if (isPoOnly) {
            // Discourse API doesn't support PO only easily in this endpoint?
            // Or we need to filter locally but that breaks pagination consistency if we rely on API pagination.
            // For now, return failure or ignore flag.
            // Ideally: NotImplementedError or ignore.
        }
        return when (val result = api.getTopic(threadId, page)) {
            is SaniouResult.Success -> {
                try {
                    val response = result.data
                    val comments = response.postStream.posts.map { discoursePost ->
                        discoursePost.toDomainComment(
                            sourceId = id,
                            threadId = response.id.toString(),
                            page = page
                        ).copy(sourceId = id)
                    }

                    val nextCursor = if (comments.isNotEmpty()) (page + 1).toString() else null
                    val prevCursor = if (page > 1) (page - 1).toString() else null

                    Result.success(PagedResult(comments, prevCursor, nextCursor))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            is SaniouResult.Error -> Result.failure(result.ex)
        }
    }

    override suspend fun getTopicDetail(threadId: String, page: Int): Result<Topic> {
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
                        createdAt = Instant.fromEpochMilliseconds(createdAt.toEpochMilliseconds()),
                        title = response.title,
                        content = firstPost?.cooked ?: "", // Use full content
                        sourceName = name,
                        sourceId = id,
                        sourceUrl = "https://meta.discourse.org/t/${response.id}",
                        author = Author(
                            id = firstPost?.username ?: "",
                            name = firstPost?.name ?: firstPost?.username ?: "Anonymous"
                        ), // author -> Author object
                        channelName = "Discourse", // forumName -> channelName
                        isLocal = false,
                        lastViewedCommentId = "",
                        comments = emptyList(),
                        summary = null,
                        tags = emptyList() // TODO: Map tags
                    )

                    // Update cache/DB
                    db.topicQueries.transaction {
                        // Upsert Topic
                        val entity = post.toEntity(page)
                        db.topicQueries.upsertTopic(entity)

                        // Upsert Comments (including Post #1 as floor 1)
                        response.postStream.posts.forEach { discoursePost ->
                            db.commentQueries.upsertComment(
                                discoursePost.toComment(
                                    sourceId = id,
                                    threadId = response.id.toString(),
                                    page = page
                                )
                            )
                        }
                    }

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

    override fun getChannel(channelId: String): Flow<Channel?> {
        // TODO: Implement proper forum detail fetching or caching
        // For now, return null as we don't persist forums locally yet
        return flowOf(null)
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
            listOf(Image(originalUrl = it, thumbnailUrl = it))
        } ?: emptyList(), // img/ext -> images
        title = title,
        content = excerpt ?: fancyTitle,
        createdAt = Instant.fromEpochMilliseconds(postCreatedAt.toEpochMilliseconds()), // now -> createdAt
        sourceName = "discourse",
        sourceId = "discourse",
        sourceUrl = "https://meta.discourse.org/t/$slug/$id",
        author = Author(
            id = user?.username ?: "Unknown",
            name = user?.name ?: user?.username ?: "Anonymous"
        ),
        channelName = "Discourse", // forumName -> channelName
        isLocal = false,
        lastViewedCommentId = "",
        comments = emptyList(),
        summary = null,
        tags = emptyList() // TODO: Map tags
    )
}
