package ai.saniou.nmb.domain

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.FavoriteForumType
import ai.saniou.nmb.data.entity.ForumCategory
import ai.saniou.nmb.data.entity.ForumDetail
import ai.saniou.nmb.data.entity.toForumCategory
import ai.saniou.nmb.data.entity.toForumDetail
import ai.saniou.nmb.data.entity.toTable
import ai.saniou.nmb.data.entity.toTimeLine
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.data.entity.RemoteKeyType
import ai.saniou.nmb.db.Database
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
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
    suspend operator fun invoke(): List<ForumCategory> = coroutineScope {

        //如果超出一天时间
        val now = Clock.System.now()
        val lastQueryTime =
            db.remoteKeyQueries.getRemoteKeyById(
                RemoteKeyType.FORUM_CATEGORY,
                RemoteKeyType.FORUM_CATEGORY.name
            ).executeAsOneOrNull()?.updateAt ?: 0L

        val lastUpdateInstant = Instant.fromEpochMilliseconds(lastQueryTime)
        val needUpdateForumCache = now - lastUpdateInstant >= 1.days


        val forumList = async {
            if (needUpdateForumCache)
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
                            forumCategory.forums.forEach { forumDetail ->
                                db.forumQueries.insertForum(forumDetail.toTable())
                            }
                        }
                    }

                    is SaniouResponse.Error -> throw forumList.ex
                }
            else db.forumQueries.getAllForumCategory().executeAsList()
                .map {
                    it.toForumCategory(db.forumQueries.getGroupForum(it.id).executeAsList())
                }
        }
        val timeLineList = async {
            if (needUpdateForumCache)
                when (val forumList = forumRepository.getTimelineList()) {
                    is SaniouResponse.Success -> forumList.data.also { timeLines ->
                        timeLines.forEach { timeLine ->
                            db.timeLineQueries.insertTimeLine(timeLine.toTable())
                        }
                    }

                    is SaniouResponse.Error -> throw forumList.ex
                }
            else
                db.timeLineQueries.getAllTimeLines().executeAsList().map { it.toTimeLine() }
        }

        if (needUpdateForumCache) {
            db.remoteKeyQueries.insertKey(
                type = RemoteKeyType.FORUM_CATEGORY,
                id = RemoteKeyType.FORUM_CATEGORY.name,
                nextKey = null,
                prevKey = null,
                updateAt = now.toEpochMilliseconds(),
            )
        }

        buildList {
            add(
                ForumCategory(
                    id = -1,
                    sort = -1,
                    name = "时间线",
                    status = "n",
                    forums = timeLineList.await().map { timeLine ->
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

            addAll(forumList.await())
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
