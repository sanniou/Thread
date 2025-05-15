package ai.saniou.nmb.domain

import ai.saniou.nmb.data.entity.ShowF
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.data.source.ForumPagingSource
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class ForumUserCase(
    private val forumRepository: ForumRepository,
    private val forumPagingSource: ForumPagingSource
) {
    operator fun invoke(
        id: Long, page: Long
    ): Flow<PagingData<ShowF>> {
        forumPagingSource.initCuisine(id)
        return Pager(
            config = PagingConfig(pageSize = 20, prefetchDistance = 3),
            pagingSourceFactory = { forumPagingSource }
        ).flow

    }
}
