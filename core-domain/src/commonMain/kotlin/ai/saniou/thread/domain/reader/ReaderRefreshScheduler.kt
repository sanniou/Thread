package ai.saniou.thread.domain.reader

import ai.saniou.thread.domain.model.reader.ReaderRefreshReport
import ai.saniou.thread.domain.model.reader.ReaderSchedulerState
import kotlinx.coroutines.flow.StateFlow

interface ReaderRefreshScheduler {
    val state: StateFlow<ReaderSchedulerState>
    fun start()
    fun stop()
    suspend fun refreshDueNow(): ReaderRefreshReport
}
