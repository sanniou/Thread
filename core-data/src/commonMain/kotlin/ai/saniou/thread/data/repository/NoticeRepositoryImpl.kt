package ai.saniou.thread.data.repository

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toDomain() }
    }

    override suspend fun fetchAndCacheNotice() {
        val lastFetchTime = settingsRepository.getValue<Long>(KEY_LAST_FETCH_TIME) ?: 0
        if (Clock.System.now().toEpochMilliseconds() - lastFetchTime > 1.days.inWholeMilliseconds) {
            val response = api.notice()
            when (response) {
                is SaniouResult.Success -> {
                    db.noticeQueries.insertNotice(
                        id = response.data.content.hashCode().toString(),
                        content = response.data.content,
                        date = Clock.System.now().toEpochMilliseconds(),
                        enable = if (response.data.enable) 1 else 0,
                    )
                    settingsRepository.saveValue(KEY_LAST_FETCH_TIME, Clock.System.now().toEpochMilliseconds())
                }

                is SaniouResult.Error ->
                    throw response.ex
            }
        }
    }

    override suspend fun markAsRead(id: String) {
        db.noticeQueries.markAsRead(id)
    }

    companion object {
        private const val KEY_LAST_FETCH_TIME = "key_last_fetch_time_notice"
    }
}
