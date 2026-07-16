package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.db.Database
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.domain.model.forum.Notice
import ai.saniou.thread.domain.repository.NoticeRepository
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.getValue
import ai.saniou.thread.domain.repository.saveValue
import ai.saniou.thread.network.SaniouResult
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class NoticeRepositoryImpl(
    private val api: NmbXdApi,
    private val db: Database,
    private val settingsRepository: SettingsRepository,
) : NoticeRepository {

    override suspend fun getLatestNotice(): Flow<Notice?> {
        return db.noticeQueries.getLatestNotice()
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }
    }

    override suspend fun fetchAndCacheNotice() {
        val lastFetchTime = settingsRepository.getValue<Long>(KEY_LAST_FETCH_TIME) ?: 0
        if (Clock.System.now().toEpochMilliseconds() - lastFetchTime > 1.days.inWholeMilliseconds) {
            val response = api.notice()
            when (response) {
                is SaniouResult.Success -> {
                    val id = response.data.content.hashCode().toString()
                    val now = Clock.System.now().toEpochMilliseconds()
                    db.noticeQueries.insertNotice(
                        id = id,
                        content = response.data.content,
                        date = now,
                        enable = if (response.data.enable) 1 else 0,
                    )
                    if (response.data.enable) {
                        val muted = db.inboxEventQueries.getInboxSourcePreference("nmb")
                            .executeAsOneOrNull()?.muted ?: 0L
                        db.inboxEventQueries.upsertInboxEvent(
                            id = "notice:nmb:$id",
                            kind = "ANNOUNCEMENT",
                            sourceId = "nmb",
                            title = "社区公告",
                            summary = response.data.content.take(320),
                            contentKind = null,
                            contentId = null,
                            contentSourceId = null,
                            parentId = null,
                            canonicalUrl = null,
                            occurredAt = now,
                            readAt = null,
                            muted = muted,
                            priority = 2,
                        )
                    }
                    settingsRepository.saveValue(KEY_LAST_FETCH_TIME, Clock.System.now().toEpochMilliseconds())
                }

                is SaniouResult.Error ->
                    throw response.ex
            }
        }
    }

    override suspend fun markAsRead(id: String) {
        db.noticeQueries.markAsRead(id)
        db.inboxEventQueries.markInboxRead(
            readAt = Clock.System.now().toEpochMilliseconds(),
            id = "notice:nmb:$id",
        )
    }

    companion object {
        private const val KEY_LAST_FETCH_TIME = "key_last_fetch_time_notice"
    }
}
