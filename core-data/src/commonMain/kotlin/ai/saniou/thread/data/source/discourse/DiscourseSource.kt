package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.mapper.flatten
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toDomainTree
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopic
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseUser
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseSearchPost
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseUserAction
import ai.saniou.thread.data.source.discourse.remote.dto.extractDiscourseImages
import ai.saniou.thread.data.source.discourse.remote.dto.resolveDiscourseUrl
import ai.saniou.thread.data.source.discourse.remote.dto.toDomainComment
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.PagedResult
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.model.user.LoginField
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.observeValue
import ai.saniou.thread.domain.repository.saveValue
import ai.saniou.thread.domain.source.ForumSearchConnector
import ai.saniou.thread.domain.source.UserContentConnector
import ai.saniou.thread.network.SaniouResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Topic
import kotlinx.coroutines.flow.flowOf
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState

class DiscourseSource(
    private val api: DiscourseApi,
    private val cache: SourceCache,
    private val db: Database,
    private val settingsRepository: SettingsRepository,
    connection: DiscourseConnectionConfig,
) : Source, ForumSearchConnector, UserContentConnector {
    override val id: String = connection.sourceId
    override val sourceId: String = id
    override val name: String = connection.displayName
    override val capabilities = ai.saniou.thread.domain.model.SourceCapabilities(
        supportsSearch = true,
        supportsTopicCreation = true,
        supportsReplies = true,
        supportsUserContent = true,
        supportsLogin = true,
        commentPageSize = 20,
    )
    private val baseUrl: String = connection.baseUrl.trimEnd('/')
    override fun topicUrl(topicId: String): String = "$baseUrl/t/$topicId"
    override fun getFeedCursor(page: Int): String? = if (page <= 1) null else (page - 1).toString()

    override val isInitialized: Flow<Boolean> =
        settingsRepository.observeValue<Boolean>("${id}_initialized")
            .map { it == true }

    override val loginStrategy: LoginStrategy = LoginStrategy.Api(
        title = "Discourse API 登录",
        description = "输入 User API Key；测试凭据仍作为默认值保留，可在这里随时覆盖。",
        fields = listOf(
            LoginField("apiKey", "User API Key", "粘贴 Discourse User API Key", isMultiline = true),
            LoginField("alias", "账号备注", "例如：Linux.do 测试账号", isRequired = false),
        ),
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
        return try {
            // 1. Try to fetch from network
            when (val response = api.getCategories()) {
                is SaniouResult.Success -> {
                    val forums = response.data.categoryList.categories.map { it.toDomainTree(id, name) }

                    // 2. Save to cache (flatten first)
                    val flatForums = forums.flatMap { it.flatten() }
                    cache.saveChannels(flatForums.map { it.toEntity() })
                    settingsRepository.saveValue("${id}_initialized", true)

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
            buildTree(it.copy(groupName = name))
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
        val page = cursor?.toIntOrNull()
            ?: 0 // Discourse pages start at 0? Or 1? Usually 0 for some APIs, but let's assume 0 based on previous code usage?
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
                    val categoryNames = cache.getChannels(id).associate { it.id to it.name }
                    val topics = result.data.topicList.topics.map { topic ->
                        topic.toPost(usersMap, id, name, baseUrl, categoryNames)
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
        return when (val result = api.getTopic(threadId, page)) {
            is SaniouResult.Success -> {
                try {
                    val response = result.data
                    val postIdsByNumber = response.postStream.posts.associate {
                        it.postNumber to it.id.toString()
                    }
                    val comments = response.postStream.posts.map { discoursePost ->
                        discoursePost.toDomainComment(
                            sourceId = id,
                            sourceName = name,
                            threadId = response.id.toString(),
                            sourceBaseUrl = baseUrl,
                            replyTargetId = discoursePost.replyToPostNumber?.let(postIdsByNumber::get),
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
                    val channel = cache.getChannels(id).firstOrNull { it.id == response.categoryId.toString() }
                    val postIdsByNumber = response.postStream.posts.associate {
                        it.postNumber to it.id.toString()
                    }
                    val comments = response.postStream.posts.drop(1).map { discoursePost ->
                        discoursePost.toDomainComment(
                            sourceId = id,
                            sourceName = name,
                            threadId = response.id.toString(),
                            sourceBaseUrl = baseUrl,
                            replyTargetId = discoursePost.replyToPostNumber?.let(postIdsByNumber::get),
                        )
                    }
                    val post = Topic(
                        id = response.id.toString(),
                        channelId = response.categoryId.toString(), // fid -> channelId
                        commentCount = response.replyCount.toLong(), // replyCount -> commentCount
                        images = firstPost?.cooked.orEmpty().extractDiscourseImages(baseUrl),
                        createdAt = Instant.fromEpochMilliseconds(createdAt.toEpochMilliseconds()),
                        title = response.title,
                        content = firstPost?.cooked ?: "", // Use full content
                        sourceName = name,
                        sourceId = id,
                        sourceUrl = topicUrl(response.id.toString()),
                        author = Author(
                            id = firstPost?.username ?: "",
                            name = firstPost?.name ?: firstPost?.username ?: "Anonymous",
                            avatar = firstPost?.avatarTemplate?.replace("{size}", "120")
                                ?.resolveDiscourseUrl(baseUrl),
                            sourceName = name,
                        ),
                        channelName = channel?.name ?: name,
                        isLocal = false,
                        lastViewedCommentId = "",
                        comments = comments,
                        summary = null,
                        tags = response.tags.map { tag -> discourseTag(id, baseUrl, tag) },
                    )
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
        return cache.observeChannels(id).map { channels ->
            channels.firstOrNull { it.id == channelId }?.let { selected ->
                val children = channels.filter { it.parentId == selected.id }
                selected.toDomain(db.channelQueries).copy(children = children.map { it.toDomain(db.channelQueries) })
            }
        }
    }

    override fun searchTopics(query: String): Flow<androidx.paging.PagingData<Topic>> = Pager(
        config = PagingConfig(pageSize = DISCOURSE_PAGE_SIZE),
        pagingSourceFactory = {
            DiscourseCapabilityPagingSource { page ->
                api.search(query, page).dataOrThrow().topics.map { topic ->
                    topic.toPost(emptyMap(), id, name, baseUrl)
                }
            }
        },
    ).flow

    override fun searchComments(query: String): Flow<androidx.paging.PagingData<Comment>> = Pager(
        config = PagingConfig(pageSize = DISCOURSE_PAGE_SIZE),
        pagingSourceFactory = {
            DiscourseCapabilityPagingSource { page ->
                api.search(query, page).dataOrThrow().posts.map { it.toDomainComment(id, name, baseUrl) }
            }
        },
    ).flow

    override fun getUserTopics(userId: String): Flow<androidx.paging.PagingData<Topic>> = Pager(
        config = PagingConfig(pageSize = DISCOURSE_PAGE_SIZE),
        pagingSourceFactory = {
            DiscourseCapabilityPagingSource { page ->
                api.getUserActions(
                    username = userId,
                    filter = USER_ACTION_TOPICS,
                    offset = (page - 1) * DISCOURSE_PAGE_SIZE,
                ).dataOrThrow().userActions.map { it.toDomainTopic(id, name, baseUrl) }
            }
        },
    ).flow

    override fun getUserComments(userId: String): Flow<androidx.paging.PagingData<Comment>> = Pager(
        config = PagingConfig(pageSize = DISCOURSE_PAGE_SIZE),
        pagingSourceFactory = {
            DiscourseCapabilityPagingSource { page ->
                api.getUserActions(
                    username = userId,
                    filter = USER_ACTION_POSTS,
                    offset = (page - 1) * DISCOURSE_PAGE_SIZE,
                ).dataOrThrow().userActions.map { it.toDomainComment(id, name, baseUrl) }
            }
        },
    ).flow
}

internal fun DiscourseTopic.toPost(
    usersMap: Map<Long, DiscourseUser>,
    sourceId: String,
    sourceName: String,
    sourceBaseUrl: String,
    categoryNames: Map<String, String> = emptyMap(),
): Topic {
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
        images = imageUrl?.resolveDiscourseUrl(sourceBaseUrl)?.let {
            listOf(Image(originalUrl = it, thumbnailUrl = it))
        } ?: emptyList(), // img/ext -> images
        title = title,
        content = excerpt.orEmpty(),
        createdAt = Instant.fromEpochMilliseconds(postCreatedAt.toEpochMilliseconds()), // now -> createdAt
        sourceName = sourceName,
        sourceId = sourceId,
        sourceUrl = "$sourceBaseUrl/t/$slug/$id",
        author = Author(
            id = user?.username ?: "Unknown",
            name = user?.name ?: user?.username ?: "Anonymous",
            avatar = user?.avatarTemplate?.replace("{size}", "80")?.resolveDiscourseUrl(sourceBaseUrl),
            sourceName = sourceName,
        ),
        channelName = categoryNames[categoryId.toString()] ?: sourceName,
        isLocal = false,
        lastViewedCommentId = "",
        comments = emptyList(),
        summary = excerpt,
        tags = tags.orEmpty().map { tag ->
            ai.saniou.thread.domain.model.Tag(
                id = "$sourceId:$tag",
                name = tag,
                url = "$sourceBaseUrl/tag/$tag",
            )
        },
        lastReplyAt = lastPostedAt.parseInstant().toEpochMilliseconds(),
    )
}

private const val DISCOURSE_PAGE_SIZE = 30
private const val USER_ACTION_TOPICS = 4
private const val USER_ACTION_POSTS = 5

private suspend fun <T> SaniouResult<T>.dataOrThrow(): T = when (this) {
    is SaniouResult.Success -> data
    is SaniouResult.Error -> throw ex
}

private class DiscourseCapabilityPagingSource<T : Any>(
    private val loadPage: suspend (Int) -> List<T>,
) : PagingSource<Int, T>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> = try {
        val page = params.key ?: 1
        val data = loadPage(page)
        LoadResult.Page(
            data = data,
            prevKey = if (page > 1) page - 1 else null,
            nextKey = if (data.isEmpty()) null else page + 1,
        )
    } catch (error: Throwable) {
        LoadResult.Error(error)
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? = state.anchorPosition?.let { anchor ->
        state.closestPageToPosition(anchor)?.let { page ->
            page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
        }
    }
}

private fun DiscourseSearchPost.toDomainComment(
    sourceId: String,
    sourceName: String,
    sourceBaseUrl: String,
): Comment = Comment(
    id = id.toString(),
    sourceId = sourceId,
    topicId = topicId.toString(),
    author = Author(
        id = username,
        name = name ?: username,
        avatar = avatarTemplate.avatarUrl()?.resolveDiscourseUrl(sourceBaseUrl),
        sourceName = sourceName,
    ),
    createdAt = createdAt.parseInstant(),
    content = cooked.ifBlank { blurb },
    floor = postNumber.toLong(),
    replyToId = null,
    agreeCount = 0,
    disagreeCount = 0,
    subCommentCount = 0,
    authorLevel = 0,
    isPo = false,
    images = cooked.extractDiscourseImages(sourceBaseUrl),
    isAdmin = false,
    title = null,
    subCommentsPreview = emptyList(),
)

private fun DiscourseUserAction.toDomainTopic(
    sourceId: String,
    sourceName: String,
    sourceBaseUrl: String,
): Topic = Topic(
    id = topicId.toString(),
    channelId = "0",
    commentCount = 0,
    images = excerpt.extractDiscourseImages(sourceBaseUrl),
    createdAt = createdAt.parseInstant(),
    title = title,
    content = excerpt,
    sourceName = sourceName,
    sourceId = sourceId,
    sourceUrl = "$sourceBaseUrl/t/$slug/$topicId",
    author = Author(
        id = username,
        name = name ?: username,
        avatar = avatarTemplate.avatarUrl()?.resolveDiscourseUrl(sourceBaseUrl),
        sourceName = sourceId,
    ),
    channelName = sourceName,
    isLocal = false,
    lastViewedCommentId = "",
    comments = emptyList(),
    summary = null,
    tags = emptyList(),
)

private fun DiscourseUserAction.toDomainComment(
    sourceId: String,
    sourceName: String,
    sourceBaseUrl: String,
): Comment = Comment(
    id = (postId ?: topicId).toString(),
    sourceId = sourceId,
    topicId = topicId.toString(),
    author = Author(
        id = username,
        name = name ?: username,
        avatar = avatarTemplate.avatarUrl()?.resolveDiscourseUrl(sourceBaseUrl),
        sourceName = sourceName,
    ),
    createdAt = createdAt.parseInstant(),
    content = excerpt,
    floor = postNumber.toLong(),
    replyToId = null,
    agreeCount = 0,
    disagreeCount = 0,
    subCommentCount = 0,
    authorLevel = 0,
    isPo = postNumber == 1,
    images = excerpt.extractDiscourseImages(sourceBaseUrl),
    isAdmin = false,
    title = title,
    subCommentsPreview = emptyList(),
)

private fun String.parseInstant(): Instant = runCatching { Instant.parse(this) }
    .getOrElse { Instant.fromEpochMilliseconds(0) }

private fun String.avatarUrl(): String? = takeIf { it.isNotBlank() }?.replace("{size}", "80")

private fun discourseTag(sourceId: String, baseUrl: String, name: String) = ai.saniou.thread.domain.model.Tag(
    id = "$sourceId:$name",
    name = name,
    url = "$baseUrl/tag/$name",
)
