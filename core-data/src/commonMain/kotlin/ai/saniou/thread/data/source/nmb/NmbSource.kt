package ai.saniou.thread.data.source.nmb

import ai.saniou.corecommon.utils.toTime
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.dto.Forum
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.data.source.nmb.remote.dto.Reply
import ai.saniou.thread.data.source.nmb.remote.dto.Thread
import ai.saniou.thread.data.source.nmb.remote.dto.ThreadReply
import ai.saniou.thread.data.source.nmb.remote.dto.ThreadWithInformation
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.data.source.nmb.remote.dto.toTableReply
import ai.saniou.thread.data.source.nmb.remote.dto.toThreadWithInformation
import ai.saniou.thread.data.manager.CdnManager
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.Cookie
import ai.saniou.thread.db.table.forum.Image
import ai.saniou.thread.domain.model.forum.ImageType
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.SourceCapabilities
import ai.saniou.thread.domain.repository.observeValue
import ai.saniou.thread.network.SaniouResponse
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import ai.saniou.thread.db.table.forum.GetTopicsInChannelOffset as GetThreadsInForumOffset
import ai.saniou.thread.domain.model.forum.Channel as DomainForum
import ai.saniou.thread.domain.model.forum.Comment as DomainComment
import ai.saniou.thread.domain.model.forum.Topic as Post

class NmbSource(
    private val nmbXdApi: NmbXdApi,
    private val db: Database,
    private val settingsRepository: SettingsRepository,
    private val cdnManager: CdnManager,
) : Source {
    override val id: String = "nmb"
    override val name: String = "A岛"

    override val capabilities: SourceCapabilities = SourceCapabilities(
        supportsTrend = true,
        supportsTrendHistory = true,
        supportsPagination = true
    )

    override val isInitialized: Flow<Boolean> =
        settingsRepository.observeValue<Boolean>("nmb_initialized")
            .map { it == true }

    @OptIn(ExperimentalTime::class)
    override suspend fun getForums(): Result<List<DomainForum>> {
        // 1. Check cache policy
        val now = Clock.System.now()
        val lastQueryTime =
            db.remoteKeyQueries.getRemoteKeyById(
                RemoteKeyType.FORUM_CATEGORY,
                RemoteKeyType.FORUM_CATEGORY.name
            ).executeAsOneOrNull()?.updateAt ?: 0L

        val lastUpdateInstant = Instant.fromEpochMilliseconds(lastQueryTime)
        val needUpdate = now - lastUpdateInstant >= 1.days

        // 2. If cache is outdated, fetch from remote and update database
        if (needUpdate) {
            val remoteResult = fetchAndStoreRemoteForums()
            if (remoteResult.isFailure) {
                return Result.failure(remoteResult.exceptionOrNull()!!)
            }
            db.remoteKeyQueries.insertKey(
                type = RemoteKeyType.FORUM_CATEGORY,
                id = RemoteKeyType.FORUM_CATEGORY.name,
                nextKey = null,
                currKey = Long.MIN_VALUE,
                prevKey = null,
                updateAt = now.toEpochMilliseconds(),
            )
        }

        // 3. Query from database and return
        val forumsFromDb = db.channelQueries.getChannelsBySource(id).executeAsList()
        val timelines = db.timeLineQueries.getAllTimeLines().executeAsList()
        val categories =
            db.channelQueries.getChannelCategoriesBySource(id).executeAsList().associateBy { it.id }

        val forums = forumsFromDb.map { forum ->
            forum.toDomain().copy(
                groupName = categories[forum.fGroup]?.name ?: "未知分类"
            )
        }

        val combined = buildList {
            addAll(forums)
            addAll(timelines.map { it.toDomain() })
        }
        return Result.success(combined)
    }

    private suspend fun fetchAndStoreRemoteForums(): Result<Unit> {
        // Fetch forums
        when (val forumListResponse = nmbXdApi.getForumList()) {
            is SaniouResponse.Success -> {
                val forumCategories = forumListResponse.data.map { category ->
                    category.copy(forums = category.forums.filter { it.id > 0 })
                }
                forumCategories.forEach { forumCategory ->
                    db.channelQueries.insertChannelCategory(forumCategory.toTable())
                    forumCategory.forums.forEach { forumDetail ->
                        db.channelQueries.insertChannel(forumDetail.toTable())
                    }
                }
            }

            is SaniouResponse.Error -> return Result.failure(forumListResponse.ex)
        }

        // Fetch timelines
        when (val timelineListResponse = nmbXdApi.getTimelineList()) {
            is SaniouResponse.Success -> {
                timelineListResponse.data.forEach { timeLine ->
                    db.timeLineQueries.insertTimeLine(timeLine.toTable())
                }
            }

            is SaniouResponse.Error -> return Result.failure(timelineListResponse.ex)
        }
        return Result.success(Unit)
    }


    override suspend fun getPosts(forumId: String, page: Int): Result<List<Post>> {
        TODO("Not yet implemented")
    }

    override fun getThreadsPager(
        forumId: String,
        isTimeline: Boolean,
        initialPage: Int,
    ): Flow<PagingData<Post>> {
        val fidLong =
            forumId.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf(PagingData.empty())
        val pagerFlow = if (isTimeline) {
            getTimelinePager(fidLong, DataPolicy.NETWORK_ELSE_CACHE, initialPage)
        } else {
            getShowfPager(fidLong, DataPolicy.NETWORK_ELSE_CACHE, initialPage)
        }
        return pagerFlow
    }

    override suspend fun getThreadDetail(threadId: String, page: Int): Result<Post> {
        return try {
            val tid = threadId.toLongOrNull()
                ?: return Result.failure(IllegalArgumentException("Invalid NMB thread ID"))

            val response = nmbXdApi.thread(tid, page.toLong())
            if (response is SaniouResponse.Success) {
                val thread = response.data
                // Use default emptyList for images from remote since we don't have ImageQueries here for remote data yet
                // Or map remote images if available in DTO
                Result.success(thread.toDomain())
            } else {
                Result.failure((response as SaniouResponse.Error).ex)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getThreadRepliesPager(
        threadId: String,
        initialPage: Int,
        isPoOnly: Boolean,
    ): Flow<PagingData<DomainComment>> {
        val tid =
            threadId.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf(PagingData.empty())
        return getThreadRepliesPager(
            tid,
            null,
            DataPolicy.NETWORK_ELSE_CACHE,
            initialPage,
            isPoOnly
        )
    }

    override fun getForum(forumId: String): Flow<DomainForum?> {
        return db.channelQueries.getChannel(id = forumId, sourceId = id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain() }
    }

    fun getTimelinePager(
        fid: Long,
        policy: DataPolicy,
        initialPage: Int = 1,
    ): Flow<PagingData<Post>> {
        return createPager(
            fid = fid,
            policy = policy,
            initialPage = initialPage,
            fetcher = { page -> nmbXdApi.timeline(fid, page.toLong()) }
        ).flow.map { pagingData ->
            pagingData.map {
                it.toDomain(
                    db.commentQueries,
                    db.imageQueries
                )
            }
        }
    }

    fun getShowfPager(
        fid: Long,
        policy: DataPolicy,
        initialPage: Int = 1,
    ): Flow<PagingData<Post>> {
        return createPager(
            fid = fid,
            policy = policy,
            initialPage = initialPage,
            fetcher = { page -> nmbXdApi.showf(fid, page.toLong()) }
        ).flow.map { pagingData ->
            pagingData.map {
                it.toDomain(
                    db.commentQueries,
                    db.imageQueries
                )
            }
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    fun getThreadRepliesPager(
        threadId: Long,
        poUserHash: String?,
        policy: DataPolicy,
        initialPage: Int = 1,
        isPoOnly: Boolean = false,
    ): Flow<PagingData<DomainComment>> {
        val pageSize = 19
        return Pager(
            config = PagingConfig(pageSize = pageSize), // 每页19个回复
            initialKey = ((initialPage - 1) * pageSize),
            remoteMediator = ThreadRemoteMediator(
                sourceId = id,
                threadId = threadId.toString(),
                db = db,
                dataPolicy = policy,
                initialPage = initialPage,
                isPoOnly = isPoOnly,
                cdnManager = cdnManager,
                fetcher = { page ->
                    if (isPoOnly) {
                        nmbXdApi.po(threadId, page.toLong())
                    } else {
                        nmbXdApi.thread(threadId, page.toLong())
                    }
                }
            ),
            pagingSourceFactory = {
                if (isPoOnly) {
                    QueryPagingSource(
                        transacter = db.commentQueries,
                        context = Dispatchers.Default,
                        countQuery = db.commentQueries.countCommentsByTopicIdPoMode(
                            sourceId = id,
                            topicId = threadId.toString()
                        ),
                        queryProvider = { limit, offset ->
                            db.commentQueries.getCommentsByTopicIdPoModeOffset(
                                sourceId = id,
                                topicId = threadId.toString(),
                                limit = limit,
                                offset = offset,
                            )
                        }
                    )
                } else if (poUserHash != null) {
                    // 只看指定PO (Old logic, maybe deprecated or specific use case)
                    QueryPagingSource(
                        transacter = db.commentQueries,
                        context = Dispatchers.Default,
                        countQuery =
                            db.commentQueries.countCommentsByTopicIdAndUserHash(
                                id,
                                threadId.toString(),
                                poUserHash
                            ),
                        queryProvider = { limit, offset ->
                            db.commentQueries.getCommentsByTopicIdAndUserHashOffset(
                                sourceId = id,
                                topicId = threadId.toString(),
                                userHash = poUserHash,
                                limit = limit,
                                offset = offset,
                            )
                        }
                    )
                } else {
                    // 查看全部
                    QueryPagingSource(
                        transacter = db.commentQueries,
                        context = Dispatchers.Default,
                        countQuery = db.commentQueries.countCommentsByTopicId(
                            id,
                            threadId.toString()
                        ),
                        queryProvider = { limit, offset ->
                            db.commentQueries.getCommentsByTopicIdOffset(
                                sourceId = id,
                                topicId = threadId.toString(),
                                limit = limit,
                                offset = offset
                            )
                        }
                    )
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain(db.imageQueries) }
        }
    }

    suspend fun updateThreadLastAccessTime(threadId: Long, time: Long) {
        db.topicQueries.updateTopicLastAccessTime(time, id, threadId.toString())
    }

    suspend fun updateThreadLastReadReplyId(threadId: Long, replyId: String) {
        db.topicQueries.updateTopicLastReadCommentId(replyId, id, threadId.toString())
    }

    fun observeIsSubscribed(subscriptionKey: String, threadId: Long): Flow<Boolean> {
        return db.subscriptionQueries.isSubscribed(subscriptionKey, id, threadId.toString())
            .asFlow()
            .mapToOne(Dispatchers.Default)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun addSubscription(subscriptionKey: String, thread: Thread) {
        db.subscriptionQueries.insertSubscription(
            subscriptionKey = subscriptionKey,
            sourceId = id,
            topicId = thread.id.toString(),
            page = 1L,
            subscriptionTime = Clock.System.now().toEpochMilliseconds(),
            isLocal = 1L
        )
    }

    suspend fun deleteSubscription(subscriptionKey: String, threadId: Long) {
        db.subscriptionQueries.deleteSubscription(subscriptionKey, id, threadId.toString())
    }

    suspend fun getSortedCookies(): List<Cookie> {
        return db.cookieQueries.getSortedCookies().asFlow().mapToList(Dispatchers.Default).first()
    }

    @OptIn(ExperimentalTime::class)
    suspend fun insertCookie(alias: String, cookie: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        val count =
            db.cookieQueries.countCookies().asFlow().mapToList(Dispatchers.Default).first().size
        db.cookieQueries.insertCookie(
            cookie = cookie,
            alias = alias,
            sort = count.toLong(),
            createdAt = now,
            lastUsedAt = now
        )
    }

    suspend fun getTrendAnchor(): Triple<Int, String, String>? {
        return db.keyValueQueries.getKeyValue("trend_anchor")
            .executeAsOneOrNull()
            ?.content
            ?.split("|")
            ?.let {
                if (it.size == 3) {
                    Triple(it[0].toInt(), it[1], it[2])
                } else null
            }
    }

    suspend fun setTrendAnchor(page: Int, startNow: String, endNow: String) {
        db.keyValueQueries.insertKeyValue("trend_anchor", "$page|$startNow|$endNow")
    }

    /**
     * 获取引用的回复内容
     */
    suspend fun getReference(refId: Long): Reply? {
        return try {
            val response = nmbXdApi.ref(refId)
            if (response is SaniouResponse.Success) {
                val reference = response.data
                // 将 NmbReference 转换为 Reply
                Reply(
                    id = reference.id,
                    fid = 0, // 引用 API 不返回 fid
                    replyCount = 0, // 引用 API 不返回回复数量
                    img = reference.img,
                    ext = reference.ext,
                    now = reference.now,
                    userHash = reference.userHash,
                    name = reference.name,
                    title = reference.title,
                    content = reference.content,
                    sage = reference.sage,
                    admin = 0, // 引用 API 不返回 admin
                    hide = reference.hide
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    private fun createPager(
        fid: Long,
        policy: DataPolicy,
        initialPage: Int,
        fetcher: suspend (page: Int) -> SaniouResponse<List<Forum>>,
    ): Pager<Int, GetThreadsInForumOffset> {
        val pageSize = 20
        return Pager(
            config = PagingConfig(pageSize = pageSize),
            initialKey = initialPage,
            remoteMediator = ForumRemoteMediator(
                sourceId = id,
                fid = fid.toString(),
                db = db,
                dataPolicy = policy,
                initialPage = initialPage,
                cdnManager = cdnManager,
                fetcher = fetcher
            ),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.topicQueries,
                    context = Dispatchers.Default,
                    countQuery = db.topicQueries.countTopicsByChannel(id, fid.toString()),
                    queryProvider = { limit, offset ->
                        db.topicQueries.getTopicsInChannelOffset(id, fid.toString(), limit, offset)
                    },
                )
            }
        )
    }

    suspend fun getThreadRepliesByPage(
        threadId: Long,
        page: Int,
    ): Result<List<ThreadReply>> {
        return try {
            when (val response = nmbXdApi.thread(threadId, page.toLong())) {
                is SaniouResponse.Success -> {
                    val thread = response.data
                    // 更新数据库
                    db.topicQueries.transaction {
                        db.topicQueries.upsertTopic(thread.toTable(id, page = page.toLong()))
                        // 保存 Topic 图片
                        saveNmbImage(
                            db = db,
                            cdnManager = cdnManager,
                            sourceId = id,
                            parentId = thread.id.toString(),
                            parentType = ImageType.Topic,
                            img = thread.img,
                            ext = thread.ext
                        )

                        thread.replies.forEach { reply ->
                            db.commentQueries.upsertComment(
                                reply.toTableReply(
                                    sourceId = id,
                                    threadId = thread.id,
                                    page = page.toLong()
                                )
                            )
                            // 保存 Comment 图片
                            saveNmbImage(
                                db = db,
                                cdnManager = cdnManager,
                                sourceId = id,
                                parentId = reply.id.toString(),
                                parentType = ImageType.Comment,
                                img = reply.img,
                                ext = reply.ext
                            )
                        }
                    }
                    Result.success(thread.replies)
                }

                is SaniouResponse.Error -> {
                    Result.failure(response.ex)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrendThread(page: Int): Result<Thread> {
        return try {
            when (val response = nmbXdApi.getTrendThread(page = page.toLong())) {
                is SaniouResponse.Success -> {
                    val thread = response.data
                    // 保存到数据库
                    db.topicQueries.transaction {
                        db.topicQueries.upsertTopic(thread.toTable(id, page = page.toLong()))
                        // 保存 Topic 图片
                        saveNmbImage(
                            db = db,
                            cdnManager = cdnManager,
                            sourceId = id,
                            parentId = thread.id.toString(),
                            parentType = ImageType.Topic,
                            img = thread.img,
                            ext = thread.ext
                        )

                        thread.replies.forEach { reply ->
                            db.commentQueries.upsertComment(
                                reply.toTableReply(
                                    sourceId = id,
                                    threadId = thread.id,
                                    page = page.toLong()
                                )
                            )
                            // 保存 Comment 图片
                            saveNmbImage(
                                db = db,
                                cdnManager = cdnManager,
                                sourceId = id,
                                parentId = reply.id.toString(),
                                parentType = ImageType.Comment,
                                img = reply.img,
                                ext = reply.ext
                            )
                        }
                    }
                    Result.success(thread)
                }

                is SaniouResponse.Error -> Result.failure(response.ex)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLocalLatestReply(threadId: Long): DomainComment? {
        return db.commentQueries.getLastFiveComments(id, threadId.toString())
            .executeAsList()
            .maxByOrNull { it.id.toLong() }
            ?.toDomain(db.imageQueries)
    }

    suspend fun getLocalReplyByDate(threadId: Long, datePattern: String): DomainComment? {
        return db.commentQueries.getCommentByTopicIdAndDate(
            sourceId = id,
            topicId = threadId.toString(),
            createdAt = datePattern.toTime().toEpochMilliseconds()
        ).executeAsOneOrNull()?.toDomain(db.imageQueries)
    }

    fun searchThreadsPager(query: String): Flow<PagingData<ThreadWithInformation>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.topicQueries,
                    context = Dispatchers.Default,
                    countQuery = db.topicQueries.countSearchTopics(query),
                    queryProvider = { limit, offset ->
                        db.topicQueries.searchTopics(query, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toThreadWithInformation(db.commentQueries) }
        }
    }

    fun searchRepliesPager(query: String): Flow<PagingData<DomainComment>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.commentQueries,
                    context = Dispatchers.Default,
                    countQuery = db.commentQueries.countSearchComments(query),
                    queryProvider = { limit, offset ->
                        db.commentQueries.searchComments(query, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain(db.imageQueries) }
        }
    }

    fun getUserThreadsPager(userHash: String): Flow<PagingData<ThreadWithInformation>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.topicQueries,
                    context = Dispatchers.Default,
                    countQuery = db.topicQueries.countTopicsByUserHash(userHash),
                    queryProvider = { limit, offset ->
                        db.topicQueries.getTopicsByUserHashOffset(userHash, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toThreadWithInformation(db.commentQueries) }
        }
    }

    fun getUserRepliesPager(userHash: String): Flow<PagingData<DomainComment>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.commentQueries,
                    context = Dispatchers.Default,
                    countQuery = db.commentQueries.countCommentsByUserHashNoTopic(userHash),
                    queryProvider = { limit, offset ->
                        db.commentQueries.getCommentsByUserHashOffset(userHash, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain(db.imageQueries) }
        }
    }
}

internal fun saveNmbImage(
    db: Database,
    cdnManager: CdnManager,
    sourceId: String,
    parentId: String,
    parentType: ImageType,
    img: String?,
    ext: String?,
) {
    if (!img.isNullOrEmpty() && !ext.isNullOrEmpty()) {
        val originalUrl = cdnManager.buildImageUrl(img, ext, false)
        val thumbUrl = cdnManager.buildImageUrl(img, ext, true)
        val path = "$img$ext"
        db.imageQueries.upsertImage(
            Image(
                id = path, // Using relative path as ID
                sourceId = sourceId,
                parentId = parentId,
                parentType = parentType,
                originalUrl = originalUrl,
                thumbnailUrl = thumbUrl,
                name = null,
                extension = ext,
                path = path,
                width = null,
                height = null,
                sortOrder = 0
            )
        )
    }
}
