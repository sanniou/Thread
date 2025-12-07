package ai.saniou.nmb.domain

import ai.saniou.thread.network.SaniouResponse
import ai.saniou.thread.data.source.nmb.remote.dto.FavoriteForumType
import ai.saniou.thread.data.source.nmb.remote.dto.ForumCategory
import ai.saniou.thread.data.source.nmb.remote.dto.ForumDetail
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.data.source.nmb.remote.dto.toForumCategory
import ai.saniou.thread.data.source.nmb.remote.dto.toForumDetail
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.data.source.nmb.remote.dto.toTimeLine
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.db.Database
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class ForumCategoryUseCase(
    private val forumRepository: ForumRepository,
    private val db: Database
) {
    @OptIn(ExperimentalTime::class)
    operator fun invoke(): Flow<List<ForumCategory>> = flow {
        //如果超出一天时间
        val now = Clock.System.now()
        val lastQueryTime =
            db.remoteKeyQueries.getRemoteKeyById(
                RemoteKeyType.FORUM_CATEGORY,
                RemoteKeyType.FORUM_CATEGORY.name
            ).executeAsOneOrNull()?.updateAt ?: 0L

        val lastUpdateInstant = Instant.fromEpochMilliseconds(lastQueryTime)
        val needUpdateForumCache = now - lastUpdateInstant >= 1.days


        if (needUpdateForumCache) {
            when (val forumList = forumRepository.getForumList()) {
                is SaniouResponse.Success -> forumList.data.apply {
                    // 过滤一个多余的时间线
                    forEach { forumCategory ->
                        forumCategory.forums = forumCategory.forums.filter { forumDetail ->
                            forumDetail.id > 0
                        }
                    }
                }.apply {
                    forEach { forumCategory ->
                        db.forumQueries.insertForumCategory(forumCategory.toTable())
                        forumCategory.forums.forEach { forumDetail ->
                            db.forumQueries.insertForum(forumDetail.toTable())
                        }
                    }
                }

                is SaniouResponse.Error -> throw forumList.ex
            }
            when (val forumList = forumRepository.getTimelineList()) {
                is SaniouResponse.Success -> forumList.data.also { timeLines ->
                    timeLines.forEach { timeLine ->
                        db.timeLineQueries.insertTimeLine(timeLine.toTable())
                    }
                }

                is SaniouResponse.Error -> throw forumList.ex
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

        val forumListFlow =
            db.forumQueries.getAllForumCategory().asFlow().mapToList(Dispatchers.IO).map { list ->
                list.map {
                    it.toForumCategory(db.forumQueries.getGroupForum(it.id).executeAsList())
                }
            }
        val timeLineListFlow =
            db.timeLineQueries.getAllTimeLines().asFlow().mapToList(Dispatchers.IO)
                .map { list -> list.map { it.toTimeLine() } }

        combine(forumListFlow, timeLineListFlow) { forumList, timeLineList ->
            buildList {
                add(
                    ForumCategory(
                        id = -1,
                        sort = -1,
                        name = "时间线",
                        status = "n",
                        forums = timeLineList.map { timeLine ->
                            ForumDetail(
                                id = timeLine.id,
                                name = timeLine.name,
                                fGroup = -1,
                                sort = -1,
                                showName = timeLine.displayName,
                                msg = timeLine.notice,
                                threadCount = timeLine.maxPage * 20,
                            )
                        },
                    )
                )
                addAll(forumList)
            }
        }.collect {
            emit(it)
        }
    }

    fun getFavoriteForums(): Flow<List<ForumDetail>> {
        return db.favoriteForumQueries.getAllFavoriteForum()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { forums -> forums.map { forum -> forum.toForumDetail() } }
    }

    @OptIn(ExperimentalTime::class)
    fun changeFavoriteForum(forum: ForumDetail) {
        if (db.favoriteForumQueries.countFavoriteForum(forum.id).executeAsOne() < 1L) {
            db.favoriteForumQueries.insertFavoriteForum(
                id = forum.id,
                favoriteTime = Clock.System.now().toEpochMilliseconds(),
                type = if (forum.fGroup == -1L) FavoriteForumType.TIMELINE else FavoriteForumType.FORUM
            )
        } else {
            db.favoriteForumQueries.deleteFavoriteForum(forum.id)
        }
    }
}
