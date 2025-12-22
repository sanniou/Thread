package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.Cookie
import ai.saniou.thread.db.table.forum.GetThreadsInForumOffset
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
import ai.saniou.thread.data.source.nmb.remote.dto.toThreadReply
import ai.saniou.thread.data.source.nmb.remote.dto.toThreadWithInformation
import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.domain.repository.Source
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
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.observeValue
import ai.saniou.thread.domain.model.forum.Forum as DomainForum

class NmbSource(
    private val nmbXdApi: NmbXdApi,
    private val db: Database,
    private val settingsRepository: SettingsRepository,
) : Source {
    override val id: String = "nmb"
    override val name: String = "A岛"

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
        val forumsFromDb = db.forumQueries.getForumsBySource(id).executeAsList()
        val timelines = db.timeLineQueries.getAllTimeLines().executeAsList()
        val categories = db.forumQueries.getForumCategoriesBySource(id).executeAsList().associateBy { it.id }

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
                    db.forumQueries.insertForumCategory(forumCategory.toTable())
                    forumCategory.forums.forEach { forumDetail ->
                        db.forumQueries.insertForum(forumDetail.toTable())
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
        return pagerFlow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun getThreadDetail(threadId: String, page: Int): Result<Post> {
        return try {
            val tid = threadId.toLongOrNull()
                ?: return Result.failure(IllegalArgumentException("Invalid NMB thread ID"))
            // NMB API returns replies list but also thread info in the structure if using `thread` endpoint?
            // Actually `nmbXdApi.thread` returns `Thread`.
            // We map `Thread` to `Post`.
            val response = nmbXdApi.thread(tid, page.toLong())
            if (response is SaniouResponse.Success) {
                // Thread to Post mapping needs to be checked.
                // Assuming `toDomain()` exists or we can construct it.
                // ThreadWithInformation has `toDomain()`. `Thread` has `toDomain()`?
                // `ai.saniou.thread.data.source.nmb.remote.dto.Thread` might not have direct toDomain to Post.
                // But `ThreadWithInformation` does.
                // `thread` endpoint returns `Thread` which contains `id`, `fid`, etc.
                // Let's assume we can map it.
                // For now, I'll use a placeholder or implement mapping if simple.
                // Actually `ThreadRepositoryImpl` uses `db.threadQueries.getThread` which returns `ThreadEntity`.
                // Here we fetch remote.
                val thread = response.data
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
        initialPage: Int
    ): Flow<PagingData<ai.saniou.thread.domain.model.forum.ThreadReply>>{
        val tid =
            threadId.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf(PagingData.empty())
        return getThreadRepliesPager(tid, null, DataPolicy.NETWORK_ELSE_CACHE, initialPage)
    }

    override fun getForum(forumId: String): Flow<DomainForum?> {
        return db.forumQueries.getForum(id = forumId, sourceId = id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain() }
    }

    fun getTimelinePager(
        fid: Long,
        policy: DataPolicy,
        initialPage: Int = 1,
    ): Flow<PagingData<ThreadWithInformation>> {
        return createPager(
            fid = fid,
            policy = policy,
            initialPage = initialPage,
            fetcher = { page -> nmbXdApi.timeline(fid.toLong(), page.toLong()) }
        ).flow.map { pagingData -> pagingData.map { it.toThreadWithInformation(db.threadReplyQueries) } }
    }

    fun getShowfPager(
        fid: Long,
        policy: DataPolicy,
        initialPage: Int = 1,
    ): Flow<PagingData<ThreadWithInformation>> {
        return createPager(
            fid = fid,
            policy = policy,
            initialPage = initialPage,
            fetcher = { page -> nmbXdApi.showf(fid, page.toLong()) }
        ).flow.map { pagingData -> pagingData.map { it.toThreadWithInformation(db.threadReplyQueries) } }
    }

    @OptIn(ExperimentalPagingApi::class)
    fun getThreadRepliesPager(
        threadId: Long,
        poUserHash: String?,
        policy: DataPolicy,
        initialPage: Int = 1,
    ): Flow<PagingData<ai.saniou.thread.domain.model.forum.ThreadReply>> {
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
                fetcher = { page -> nmbXdApi.thread(threadId, page.toLong()) }
            ),
            pagingSourceFactory = {
                if (poUserHash != null) {
                    // 只看PO
                    QueryPagingSource(
                        transacter = db.threadReplyQueries,
                        context = Dispatchers.Default,
                        countQuery =
                            db.threadReplyQueries.countRepliesByThreadIdAndUserHash(
                                id,
                                threadId.toString(),
                                poUserHash
                            ),
                        queryProvider = { limit, offset ->
                            db.threadReplyQueries.getRepliesByThreadIdAndUserHashOffset(
                                sourceId = id,
                                threadId = threadId.toString(),
                                userHash = poUserHash,
                                limit = limit,
                                offset = offset,
                            )
                        }
                    )
                } else {
                    // 查看全部
                    QueryPagingSource(
                        transacter = db.threadReplyQueries,
                        context = Dispatchers.Default,
                        countQuery = db.threadReplyQueries.countRepliesByThreadId(id, threadId.toString()),
                        queryProvider = { limit, offset ->
                            db.threadReplyQueries.getRepliesByThreadIdOffset(
                                sourceId = id,
                                threadId = threadId.toString(),
                                limit = limit,
                                offset = offset
                            )
                        }
                    )
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toThreadReply().toDomain() }
        }
    }

    suspend fun updateThreadLastAccessTime(threadId: Long, time: Long) {
        db.threadQueries.updateThreadLastAccessTime(time, id, threadId.toString())
    }

    suspend fun updateThreadLastReadReplyId(threadId: Long, replyId: Long) {
        db.threadQueries.updateThreadLastReadReplyId(replyId, id, threadId.toString())
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
            threadId = thread.id.toString(),
            page = 1L,
            subscriptionTime = Clock.System.now().epochSeconds,
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
        val now = Clock.System.now().epochSeconds
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
                fetcher = fetcher
            ),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.threadQueries,
                    context = Dispatchers.Default,
                    countQuery = db.threadQueries.countThreadsByFid(id, fid.toString()),
                    queryProvider = { limit, offset ->
                        db.threadQueries.getThreadsInForumOffset(id, fid.toString(), limit, offset)
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
                    db.threadQueries.transaction {
                        db.threadQueries.upsertThread(thread.toTable(id, page = page.toLong()))
                        thread.toTableReply(id, page.toLong())
                            .forEach(db.threadReplyQueries::upsertThreadReply)
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
                    db.threadQueries.transaction {
                        db.threadQueries.upsertThread(thread.toTable(id, page = page.toLong()))
                        thread.toTableReply(id, page.toLong())
                            .forEach(db.threadReplyQueries::upsertThreadReply)
                    }
                    Result.success(thread)
                }

                is SaniouResponse.Error -> Result.failure(response.ex)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLocalLatestReply(threadId: Long): ThreadReply? {
        // 获取最后5条回复中的第一条（假设 getLastFiveReplies 是倒序的，即最新的在前）
        // 如果不是倒序，可能需要调整。通常论坛的“最后几条”是用于列表展示，往往是倒序。
        // 根据 Thread.kt 中的 toThread 实现，它取了 getLastFiveReplies，推测是用于展示 Thread 预览，应该是包含最新回复的。
        return db.threadReplyQueries.getLastFiveReplies(id, threadId.toString())
            .executeAsList()
            .maxByOrNull { it.id.toLong() } // 确保取 ID 最大的，即最新的
            ?.toThreadReply()
    }

    fun searchThreadsPager(query: String): Flow<PagingData<ThreadWithInformation>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.threadQueries,
                    context = Dispatchers.Default,
                    countQuery = db.threadQueries.countSearchThreads(query),
                    queryProvider = { limit, offset ->
                        db.threadQueries.searchThreads(query, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toThreadWithInformation(db.threadReplyQueries) }
        }
    }

    fun searchRepliesPager(query: String): Flow<PagingData<ThreadReply>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.threadReplyQueries,
                    context = Dispatchers.Default,
                    countQuery = db.threadReplyQueries.countSearchReplies(query),
                    queryProvider = { limit, offset ->
                        db.threadReplyQueries.searchReplies(query, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toThreadReply() }
        }
    }

    fun getUserThreadsPager(userHash: String): Flow<PagingData<ThreadWithInformation>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.threadQueries,
                    context = Dispatchers.Default,
                    countQuery = db.threadQueries.countThreadsByUserHash(userHash),
                    queryProvider = { limit, offset ->
                        db.threadQueries.getThreadsByUserHashOffset(userHash, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toThreadWithInformation(db.threadReplyQueries) }
        }
    }

    fun getUserRepliesPager(userHash: String): Flow<PagingData<ThreadReply>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.threadReplyQueries,
                    context = Dispatchers.Default,
                    countQuery = db.threadReplyQueries.countRepliesByUserHash(userHash),
                    queryProvider = { limit, offset ->
                        db.threadReplyQueries.getRepliesByUserHashOffset(userHash, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toThreadReply() }
        }
    }
}
