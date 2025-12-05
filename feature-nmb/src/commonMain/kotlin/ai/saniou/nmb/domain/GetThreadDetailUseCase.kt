package ai.saniou.nmb.domain

import ai.saniou.thread.network.SaniouResponse
import ai.saniou.nmb.data.api.NmbXdApi
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.toTable
import ai.saniou.nmb.data.entity.toTableReply
import ai.saniou.nmb.db.Database
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart

data class ThreadDetail(
    val thread: Thread,
    val lastReadReplyId: Long,
)

class GetThreadDetailUseCase(
    private val db: Database,
    private val api: NmbXdApi,
) {
    /**
     * 从数据库中获取一个帖子的信息流。
     * 当 RemoteMediator 更新数据库时，这个 Flow 会自动发射最新的数据。
     * @param id 帖子 ID
     * @param forceRefresh 是否强制从网络刷新
     * @return 包含帖子信息的 Flow
     */
    operator fun invoke(id: Long, forceRefresh: Boolean = false): Flow<ThreadDetail> =
        db.threadQueries.getThread(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .mapNotNull { row ->
                row?.let {
                    ThreadDetail(
                        thread = Thread(
                            id = it.id,
                            fid = it.fid,
                            replyCount = it.replyCount,
                            img = it.img,
                            ext = it.ext,
                            now = it.now,
                            userHash = it.userHash,
                            name = it.name,
                            title = it.title,
                            content = it.content,
                            sage = it.sage,
                            admin = it.admin,
                            hide = it.hide,
                            //用于 Thread 所以不在获取最新回复
                            replies = emptyList()
                        ),
                        lastReadReplyId = it.last_read_reply_id ?: 0
                    )
                }
            }
            .onStart {
                // 如果需要强制刷新，或者数据库中没有数据，则从网络获取
                val needsFetch =
                    forceRefresh || db.threadQueries.getThread(id).executeAsOneOrNull() == null
                if (needsFetch) {
                    val result = api.thread(id, 1)
                    if (result is SaniouResponse.Success) {
                        val threadDetail = result.data
                        db.transaction {
                            db.threadQueries.upsertThread(threadDetail.toTable(1))
                            threadDetail.toTableReply(1)
                                .forEach(db.threadReplyQueries::upsertThreadReply)
                        }
                    }
                }
            }
            .flowOn(Dispatchers.IO)
}
