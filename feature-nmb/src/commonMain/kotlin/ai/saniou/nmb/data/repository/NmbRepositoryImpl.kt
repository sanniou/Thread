package ai.saniou.nmb.data.repository

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.api.NmbXdApi
import ai.saniou.nmb.data.entity.Forum
import ai.saniou.nmb.data.entity.Reply
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.ThreadReply
import ai.saniou.nmb.data.entity.ThreadWithInformation
import ai.saniou.nmb.data.entity.toThreadReply
import ai.saniou.nmb.data.entity.toThreadWithInformation
import ai.saniou.nmb.data.source.ForumRemoteMediator
import ai.saniou.nmb.data.source.SqlDelightPagingSource
import ai.saniou.nmb.data.source.ThreadRemoteMediator
import ai.saniou.nmb.db.Database
import ai.saniou.nmb.db.table.Cookie
import ai.saniou.nmb.db.table.GetThreadsInForum
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
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
            initialKey = initialPage,
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
                    SqlDelightPagingSource(
                        transacter = database.threadReplyQueries,
                        context = Dispatchers.IO,
                        countQueryProvider = {
                            database.threadReplyQueries.countRepliesByThreadIdAndUserHash(
                                threadId,
                                poUserHash
                            )
                        },
                        pageQueryProvider = { page ->
                            database.threadReplyQueries.getRepliesByThreadIdAndUserHash(
                                threadId = threadId,
                                userHash = poUserHash,
                                page = page.toLong()
                            )
                        }
                    )
                } else {
                    // 查看全部
                    SqlDelightPagingSource(
                        transacter = database.threadReplyQueries,
                        context = Dispatchers.IO,
                        countQueryProvider = {
                            database.threadReplyQueries.countRepliesByThreadId(
                                threadId
                            )
                        },
                        pageQueryProvider = { page ->
                            database.threadReplyQueries.getRepliesByThreadId(
                                threadId = threadId,
                                page = page.toLong()
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
    ): Pager<Int, GetThreadsInForum> {
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
                SqlDelightPagingSource(
                    transacter = database.threadQueries,
                    context = Dispatchers.IO,
                    countQueryProvider = { database.threadQueries.countThreadsByFid(fid) },
                    pageQueryProvider = { page ->
                        database.threadQueries.getThreadsInForum(
                            fid = fid,
                            page = page.toLong()
                        )
                    }
                )
            }
        )
    }
}
