package ai.saniou.thread.data.repository

import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.repository.HistoryRepository
import app.cash.paging.PagingData
import app.cash.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl(
    private val nmbSource: NmbSource
) : HistoryRepository {
    override fun getHistoryThreads(): Flow<PagingData<Post>> {
        return nmbSource.getHistoryThreads().map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }
}
