package ai.saniou.thread.data.source.nmb

import ai.saniou.nmb.db.Database
import ai.saniou.nmb.db.table.Cookie
import ai.saniou.nmb.db.table.GetThreadsInForumOffset
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
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import ai.saniou.thread.domain.model.forum.Forum as DomainForum

class NmbSource(
    private val nmbXdApi: NmbXdApi,
    private val db: Database,
) : Source {
    override val id: String = "nmb"

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
        val forumsFromDb = db.forumQueries.getAllForum().executeAsList()
        val timelines = db.timeLineQueries.getAllTimeLines().executeAsList()
        val categories = db.forumQueries.getAllForumCategory().executeAsList().associateBy { it.id }

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
    ): Flow<PagingData<ThreadReply>> {
        val pageSize = 19
        return Pager(
            config = PagingConfig(pageSize = pageSize), // 每页19个回复
            initialKey = ((initialPage - 1) * pageSize),
            remoteMediator = ThreadRemoteMediator(
                threadId = threadId,
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
                                threadId,
                                poUserHash
                            ),
                        queryProvider = { limit, offset ->
                            db.threadReplyQueries.getRepliesByThreadIdAndUserHashOffset(
                                threadId = threadId,
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
                        countQuery = db.threadReplyQueries.countRepliesByThreadId(threadId),
                        queryProvider = { limit, offset ->
                            db.threadReplyQueries.getRepliesByThreadIdOffset(
                                threadId,
                                limit,
                                offset
                            )
                        }
                    )
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toThreadReply() }
        }
    }

    fun getHistoryThreads(): Flow<PagingData<ThreadWithInformation>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.threadQueries,
                    context = Dispatchers.Default,
                    countQuery = db.threadQueries.countHistoryThreads(),
                    queryProvider = db.threadQueries::getHistoryThreads
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toThreadWithInformation(db.threadReplyQueries) }
        }
    }

    suspend fun updateThreadLastAccessTime(threadId: Long, time: Long) {
        db.threadQueries.updateThreadLastAccessTime(time, threadId)
    }

    suspend fun updateThreadLastReadReplyId(threadId: Long, replyId: Long) {
        db.threadQueries.updateThreadLastReadReplyId(replyId, threadId)
    }

    fun observeIsSubscribed(subscriptionKey: String, threadId: Long): Flow<Boolean> {
        return db.subscriptionQueries.isSubscribed(subscriptionKey, threadId)
            .asFlow()
            .mapToOne(Dispatchers.Default)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun addSubscription(subscriptionKey: String, thread: Thread) {
        db.subscriptionQueries.insertSubscription(
            subscriptionKey = subscriptionKey,
            threadId = thread.id,
            page = 1L,
            subscriptionTime = Clock.System.now().epochSeconds,
            isLocal = 1L
        )
    }

    suspend fun deleteSubscription(subscriptionKey: String, threadId: Long) {
        db.subscriptionQueries.deleteSubscription(subscriptionKey, threadId)
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
                sourceId = fid,
                db = db,
                dataPolicy = policy,
                initialPage = initialPage,
                fetcher = fetcher
            ),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.threadQueries,
                    context = Dispatchers.Default,
                    countQuery = db.threadQueries.countThreadsByFid(fid),
                    queryProvider = { limit, offset ->
                        db.threadQueries.getThreadsInForumOffset(fid, limit, offset)
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
                        db.threadQueries.upsertThread(thread.toTable(page = page.toLong()))
                        thread.toTableReply(page.toLong())
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
                        db.threadQueries.upsertThread(thread.toTable(page = page.toLong()))
                        thread.toTableReply(page.toLong())
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
        return db.threadReplyQueries.getLastFiveReplies(threadId)
            .executeAsList()
            .maxByOrNull { it.id } // 确保取 ID 最大的，即最新的
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
