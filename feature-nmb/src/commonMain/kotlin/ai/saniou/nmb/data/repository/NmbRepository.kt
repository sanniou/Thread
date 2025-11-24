package ai.saniou.nmb.data.repository

import ai.saniou.nmb.data.entity.Reply
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.ThreadReply
import ai.saniou.nmb.data.entity.ThreadWithInformation
import ai.saniou.nmb.db.table.Cookie
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

/**
 * NMB 仓库接口
 */
interface NmbRepository {
    companion object {
        const val REPLIES_PER_PAGE = 19
    }

    /**
     * 获取引用的回复内容
     */
    suspend fun getReference(refId: Long): Reply?

    /**
     * 更新帖子的最后访问时间
     */
    suspend fun updateThreadLastAccessTime(threadId: Long, time: Long)

    /**
     * 更新帖子的最后阅读回复ID
     */
    suspend fun updateThreadLastReadReplyId(threadId: Long, replyId: Long)

    fun observeIsSubscribed(subscriptionKey: String, threadId: Long): Flow<Boolean>
    suspend fun addSubscription(subscriptionKey: String, thread: Thread)
    suspend fun deleteSubscription(subscriptionKey: String, threadId: Long)

    suspend fun getSortedCookies(): List<Cookie>
    suspend fun insertCookie(alias: String, cookie: String)
    suspend fun deleteCookie(cookie: String)
    suspend fun updateCookiesSort(cookies: List<Cookie>)
    suspend fun getFirstCookie(): Cookie?

    fun getTimelinePager(
        fid: Long,
        policy: DataPolicy,
        initialPage: Int = 1,
    ): Flow<PagingData<ThreadWithInformation>>

    fun getShowfPager(
        fid: Long,
        policy: DataPolicy,
        initialPage: Int = 1,
    ): Flow<PagingData<ThreadWithInformation>>

    fun getThreadRepliesPager(
        threadId: Long,
        poUserHash: String?, // nullable, 为 null 时加载所有回复
        policy: DataPolicy,
        initialPage: Int = 1,
    ): Flow<PagingData<ThreadReply>>

    suspend fun getThreadRepliesByPage(threadId: Long, page: Int): Result<List<ThreadReply>>
}
