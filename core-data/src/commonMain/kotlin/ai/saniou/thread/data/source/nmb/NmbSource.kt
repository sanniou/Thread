package ai.saniou.thread.data.source.nmb

import ai.saniou.nmb.db.Database
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.network.SaniouResponse
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class NmbSource(
    private val nmbXdApi: NmbXdApi,
    private val db: Database
) : Source {
    override val id: String = "nmb"

    @OptIn(ExperimentalTime::class)
    override suspend fun getForums(): Result<List<Forum>> {
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
        val forums = db.forumQueries.getAllForum().asFlow().mapToList(Dispatchers.IO).first()
        val timelines = db.timeLineQueries.getAllTimeLines().asFlow().mapToList(Dispatchers.IO).first()

        val combined = buildList {
            addAll(forums.map { it.toDomain() })
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
}
