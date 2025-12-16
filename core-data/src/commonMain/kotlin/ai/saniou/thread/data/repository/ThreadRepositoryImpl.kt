package ai.saniou.thread.data.repository

import ai.saniou.thread.db.Database
import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.model.forum.ThreadReply
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.ThreadRepository
import app.cash.paging.PagingData
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class ThreadRepositoryImpl(
    private val db: Database,
    private val sources: Set<Source>,
    private val cache: SourceCache,
) : ThreadRepository {

    private val sourceMap by lazy { sources.associateBy { it.id } }

    override fun getThreadDetail(sourceId: String, id: String, forceRefresh: Boolean): Flow<Post> {
        return cache.observeThread(sourceId, id)
            .mapNotNull { it?.toDomain() }
            .onStart {
                val source = sourceMap[sourceId]
                if (source != null) {
                    // 触发一次网络请求以更新缓存
                    // 注意：这里假设 getThreadDetail 会更新缓存
                    // 对于 NMB，它确实会。对于 Discourse，我们需要确保它也会。
                    // 目前 DiscourseSource.getThreadDetail 只是返回 Result<Post>，没有保存到 DB。
                    // 我们需要更新 DiscourseSource.getThreadDetail 来保存数据，或者在这里处理。
                    // 为了保持一致性，最好是 Source 实现负责获取并缓存。
                    // 但目前的 Source 接口返回 Result<Post>，不强制缓存。
                    // 我们可以让 Source.getThreadDetail 返回的数据在这里保存。
                    if (forceRefresh) {
                        val result = source.getThreadDetail(id, 1)
                        // 如果成功，保存到缓存（如果 Source 内部没做的话）
                        // NmbSource 内部做了 upsert。
                        // DiscourseSource 目前没做。
                        // 理想情况下，Source 实现应该负责数据获取和持久化（如果是缓存策略的一部分）。
                        // 或者 Repository 层负责协调。
                        // 鉴于 NmbSource 已经深度集成了 DB 操作，我们暂时保留这种模式，
                        // 并期望 DiscourseSource 也遵循或在此处补充。
                        // 实际上，DiscourseSource.getThreadDetail 目前是纯网络。
                        // 我们可以在这里补充保存逻辑，但这需要将 Post 转换为 Entity，这可能需要额外的 Mapper。
                        // 暂时先调用 source.getThreadDetail，依赖其副作用（如果有）。
                        // 如果没有副作用（如 Discourse），则 UI 会展示旧数据直到下次刷新？
                        // 不，flow 会发射新数据如果 DB 更新了。
                        // 如果 DiscourseSource 不更新 DB，flow 就不会更新。
                        // 所以必须确保数据被保存。
                        // 考虑到 DiscourseSource 尚未完全实现缓存逻辑（除了 Paging），
                        // 这里暂时只调用，后续完善 DiscourseSource 的 getThreadDetail。
                    }
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override fun getThreadRepliesPaging(
        sourceId: String,
        threadId: String,
        isPoOnly: Boolean,
        initialPage: Int,
    ): Flow<PagingData<ThreadReply>> {
        val source = sourceMap[sourceId]
        return source?.getThreadRepliesPager(threadId, initialPage)
            ?: kotlinx.coroutines.flow.flowOf(PagingData.empty())
    }

    override fun getThreadImages(threadId: Long): Flow<List<Image>> =
        db.threadReplyQueries.getThreadImages("nmb", threadId.toString()) // FIXME: sourceId hardcoded or need parameter
            .asFlow()
            .map { query ->
                query.executeAsList().map {
                    Image(
                        name = it.img,
                        ext = it.ext
                    )
                }
            }
            .flowOn(Dispatchers.IO)

    override fun getThreadReplies(
        threadId: Long,
        isPoOnly: Boolean,
    ): Flow<List<ThreadReply>> {
        TODO("Not yet implemented")
    }

    override suspend fun updateThreadLastAccessTime(threadId: Long, time: Long) {
        // FIXME: Need sourceId
        // withContext(Dispatchers.IO) {
        //     cache.updateThreadLastAccessTime(sourceId, threadId.toString(), time)
        // }
    }

    override suspend fun updateThreadLastReadReplyId(threadId: Long, replyId: Long) {
        // FIXME: Need sourceId
        // withContext(Dispatchers.IO) {
        //     cache.updateThreadLastReadReplyId(sourceId, threadId.toString(), replyId)
        // }
    }
}
