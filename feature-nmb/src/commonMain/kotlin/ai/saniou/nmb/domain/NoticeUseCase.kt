package ai.saniou.nmb.domain

import ai.saniou.thread.network.SaniouResponse
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.data.storage.CommonStorage
import ai.saniou.nmb.db.Database
import ai.saniou.nmb.db.table.Notice
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

class NoticeUseCase(
    private val api: ForumRepository,
    private val db: Database,
    private val storage: CommonStorage,
) {

    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(): Flow<Notice?> {
        val lastFetchTime = storage.getValue<Long>(KEY_LAST_FETCH_TIME) ?: 0
        if (Clock.System.now().epochSeconds - lastFetchTime > 1.days.inWholeSeconds) {
            val response = api.notice()
            when (response) {
                is SaniouResponse.Success -> {
                    db.noticeQueries.insertNotice(
                        id = response.data.content.hashCode().toString(),
                        content = response.data.content,
                        date = Clock.System.now().epochSeconds,
                        enable = if (response.data.enable) 1 else 0,
                    )
                    storage.saveValue(KEY_LAST_FETCH_TIME, Clock.System.now().epochSeconds)
                }

                is SaniouResponse.Error ->
                    throw response.ex
            }
        }
        return db.noticeQueries.getLatestNotice()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
    }

    fun markAsRead(id: String) {
        db.noticeQueries.markAsRead(id)
    }

    companion object {
        private const val KEY_LAST_FETCH_TIME = "key_last_fetch_time_notice"
    }
}
