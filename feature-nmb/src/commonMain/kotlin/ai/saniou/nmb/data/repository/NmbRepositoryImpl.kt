package ai.saniou.nmb.data.repository

import ai.saniou.thread.network.SaniouResponse
import ai.saniou.nmb.data.api.NmbXdApi
import ai.saniou.nmb.data.entity.Forum
import ai.saniou.nmb.data.entity.Reply
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.ThreadReply
import ai.saniou.nmb.data.entity.ThreadWithInformation
import ai.saniou.nmb.data.entity.toTable
import ai.saniou.nmb.data.entity.toTableReply
import ai.saniou.nmb.data.entity.toThreadReply
import ai.saniou.nmb.data.entity.toThreadWithInformation
import ai.saniou.nmb.data.source.ForumRemoteMediator
import ai.saniou.nmb.data.source.SqlDelightPagingSource
import ai.saniou.nmb.data.source.ThreadRemoteMediator
import ai.saniou.nmb.db.Database
import ai.saniou.nmb.db.table.Cookie
import ai.saniou.nmb.db.table.GetThreadsInForumOffset
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
import kotlin.time.ExperimentalTime

/**
 * NMB 仓库实现类
 */
class NmbRepositoryImpl(
    private val nmbXdApi: NmbXdApi,
    private val database: Database,
) : NmbRepository, HistoryRepository {

    override fun getTimelinePager(
        fid: Long,
        policy: DataPolicy,
        initialPage: Int,
    ): Flow<PagingData<ThreadWithInformation>> {
        return createPager(
            fid = fid,
            policy = policy,
            initialPage = initialPage,
            fetcher = { page -> nmbXdApi.timeline(fid.toLong(), page.toLong()) }
        ).flow.map { pagingData -> pagingData.map { it.toThreadWithInformation(database.threadReplyQueries) } }
    }

    override fun getShowfPager(
        fid: Long,
        policy: DataPolicy,
        initialPage: Int,
    ): Flow<PagingData<ThreadWithInformation>> {
        return createPager(
            fid = fid,
            policy = policy,
            initialPage = initialPage,
            fetcher = { page -> nmbXdApi.showf(fid.toLong(), page.toLong()) }
        ).flow.map { pagingData -> pagingData.map { it.toThreadWithInformation(database.threadReplyQueries) } }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getThreadRepliesPager(
        threadId: Long,
        poUserHash: String?,
        policy: DataPolicy,
        initialPage: Int,
    ): Flow<PagingData<ThreadReply>> {
        val pageSize = 19
        return Pager(
            config = PagingConfig(pageSize = pageSize), // 每页19个回复
            initialKey = ((initialPage - 1) * pageSize),
            remoteMediator = ThreadRemoteMediator(
                threadId = threadId,
                db = database,
                dataPolicy = policy,
                initialPage = initialPage,
                fetcher = { page -> nmbXdApi.thread(threadId, page.toLong()) }
            ),
            pagingSourceFactory = {
                if (poUserHash != null) {
                    // 只看PO
                    QueryPagingSource(
                        transacter = database.threadReplyQueries,
                        context = Dispatchers.IO,
                        countQuery =
                            database.threadReplyQueries.countRepliesByThreadIdAndUserHash(
                                threadId,
                                poUserHash
                            ),
                        queryProvider = { limit, offset ->
                            database.threadReplyQueries.getRepliesByThreadIdAndUserHashOffset(
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
                        transacter = database.threadReplyQueries,
                        context = Dispatchers.IO,
                        countQuery = database.threadReplyQueries.countRepliesByThreadId(threadId),
                        queryProvider = { limit, offset ->
                            database.threadReplyQueries.getRepliesByThreadIdOffset(
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

    override fun getHistoryThreads(): Flow<PagingData<ThreadWithInformation>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                SqlDelightPagingSource(
                    transacter = database.threadQueries,
                    context = Dispatchers.IO,
                    countQueryProvider = database.threadQueries::countHistoryThreads,
                    limitOffsetQueryProvider = database.threadQueries::getHistoryThreads
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toThreadWithInformation(database.threadReplyQueries) }
        }
    }

    override suspend fun updateThreadLastAccessTime(threadId: Long, time: Long) {
        database.threadQueries.updateThreadLastAccessTime(time, threadId)
    }

    override suspend fun updateThreadLastReadReplyId(threadId: Long, replyId: Long) {
        database.threadQueries.updateThreadLastReadReplyId(replyId, threadId)
    }

    override fun observeIsSubscribed(subscriptionKey: String, threadId: Long): Flow<Boolean> {
        return database.subscriptionQueries.isSubscribed(subscriptionKey, threadId)
            .asFlow()
            .mapToOne(Dispatchers.IO)
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun addSubscription(subscriptionKey: String, thread: Thread) {
        database.subscriptionQueries.insertSubscription(
            subscriptionKey = subscriptionKey,
            threadId = thread.id,
            page = 1L,
            subscriptionTime = Clock.System.now().epochSeconds,
            isLocal = 1L
        )
    }

    override suspend fun deleteSubscription(subscriptionKey: String, threadId: Long) {
        database.subscriptionQueries.deleteSubscription(subscriptionKey, threadId)
    }

    override suspend fun getSortedCookies(): List<Cookie> {
        return database.cookieQueries.getSortedCookies().asFlow().mapToList(Dispatchers.IO).first()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun insertCookie(alias: String, cookie: String) {
        val now = Clock.System.now().epochSeconds
        val count =
            database.cookieQueries.countCookies().asFlow().mapToList(Dispatchers.IO).first().size
        database.cookieQueries.insertCookie(
            cookie = cookie,
            alias = alias,
            sort = count.toLong(),
            createdAt = now,
            lastUsedAt = now
        )
    }

    override suspend fun deleteCookie(cookie: String) {
        database.cookieQueries.deleteCookie(cookie)
    }

    override suspend fun updateCookiesSort(cookies: List<Cookie>) {
        database.cookieQueries.transaction {
            cookies.forEachIndexed { index, cookie ->
                database.cookieQueries.updateCookieSort(index.toLong(), cookie.cookie)
            }
        }
    }

    override suspend fun getFirstCookie(): Cookie? {
        return database.cookieQueries.getSortedCookies().asFlow().mapToList(Dispatchers.IO).first()
            .firstOrNull()
    }

    /**
     * 获取引用的回复内容
     */
    override suspend fun getReference(refId: Long): Reply? {
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
                db = database,
                dataPolicy = policy,
                initialPage = initialPage,
                fetcher = fetcher
            ),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = database.threadQueries,
                    context = Dispatchers.IO,
                    countQuery = database.threadQueries.countThreadsByFid(fid),
                    queryProvider = { limit, offset ->
                        database.threadQueries.getThreadsInForumOffset(fid, limit, offset)
                    },
                )
            }
        )
    }

    override suspend fun getThreadRepliesByPage(
        threadId: Long,
        page: Int,
    ): Result<List<ThreadReply>> {
        return try {
            when (val response = nmbXdApi.thread(threadId, page.toLong())) {
                is SaniouResponse.Success -> {
                    val thread = response.data
                    // 更新数据库
                    database.threadQueries.transaction {
                        database.threadQueries.upsertThread(thread.toTable(page = page.toLong()))
                        thread.toTableReply(page.toLong())
                            .forEach(database.threadReplyQueries::upsertThreadReply)
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

    override suspend fun getTrendThread(page: Int): Result<Thread> {
        return try {
            when (val response = nmbXdApi.getTrendThread(page = page.toLong())) {
                is SaniouResponse.Success -> {
                    val thread = response.data
                    // 保存到数据库
                    database.threadQueries.transaction {
                        database.threadQueries.upsertThread(thread.toTable(page = page.toLong()))
                        thread.toTableReply(page.toLong())
                            .forEach(database.threadReplyQueries::upsertThreadReply)
                    }
                    Result.success(thread)
                }

                is SaniouResponse.Error -> Result.failure(response.ex)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLocalLatestReply(threadId: Long): ThreadReply? {
        // 获取最后5条回复中的第一条（假设 getLastFiveReplies 是倒序的，即最新的在前）
        // 如果不是倒序，可能需要调整。通常论坛的“最后几条”是用于列表展示，往往是倒序。
        // 根据 Thread.kt 中的 toThread 实现，它取了 getLastFiveReplies，推测是用于展示 Thread 预览，应该是包含最新回复的。
        return database.threadReplyQueries.getLastFiveReplies(threadId)
            .executeAsList()
            .maxByOrNull { it.id } // 确保取 ID 最大的，即最新的
            ?.toThreadReply()
    }

    override fun searchThreadsPager(query: String): Flow<PagingData<ThreadWithInformation>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                SqlDelightPagingSource(
                    transacter = database.threadQueries,
                    context = Dispatchers.IO,
                    countQueryProvider = { database.threadQueries.countSearchThreads(query) },
                    limitOffsetQueryProvider = { limit, offset ->
                        database.threadQueries.searchThreads(query, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toThreadWithInformation(database.threadReplyQueries) }
        }
    }

    override fun searchRepliesPager(query: String): Flow<PagingData<ThreadReply>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                SqlDelightPagingSource(
                    transacter = database.threadReplyQueries,
                    context = Dispatchers.IO,
                    countQueryProvider = { database.threadReplyQueries.countSearchReplies(query) },
                    limitOffsetQueryProvider = { limit, offset ->
                        database.threadReplyQueries.searchReplies(query, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toThreadReply() }
        }
    }
    override fun getUserThreadsPager(userHash: String): Flow<PagingData<ThreadWithInformation>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                SqlDelightPagingSource(
                    transacter = database.threadQueries,
                    context = Dispatchers.IO,
                    countQueryProvider = { database.threadQueries.countThreadsByUserHash(userHash) },
                    limitOffsetQueryProvider = { limit, offset ->
                        database.threadQueries.getThreadsByUserHashOffset(userHash, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toThreadWithInformation(database.threadReplyQueries) }
        }
    }

    override fun getUserRepliesPager(userHash: String): Flow<PagingData<ThreadReply>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                SqlDelightPagingSource(
                    transacter = database.threadReplyQueries,
                    context = Dispatchers.IO,
                    countQueryProvider = { database.threadReplyQueries.countRepliesByUserHash(userHash) },
                    limitOffsetQueryProvider = { limit, offset ->
                        database.threadReplyQueries.getRepliesByUserHashOffset(userHash, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toThreadReply() }
        }
    }
}
