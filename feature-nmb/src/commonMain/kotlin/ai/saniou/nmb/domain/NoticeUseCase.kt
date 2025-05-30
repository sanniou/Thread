package ai.saniou.nmb.domain

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.db.Database
import ai.saniou.nmb.db.table.Notice
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class NoticeUseCase(
    private val api: ForumRepository,
    private val db: Database,
) {

    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(): Flow<Notice> {
        val response = api.notice()
        when (response) {
            is SaniouResponse.Success -> db.noticeQueries.insertNotice(
                id = response.data.content.hashCode().toString(),
                content = response.data.content,
                date = Clock.System.now().epochSeconds,
                enable = if (response.data.enable) 1 else 0,
            )

            is SaniouResponse.Error ->
                throw response.ex
        }
        return db.noticeQueries.getLatestNotice()
            .asFlow()
            .mapToOne(Dispatchers.IO)
    }
}
