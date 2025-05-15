package ai.saniou.nmb.data.source

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.ShowF
import ai.saniou.nmb.data.repository.ForumRepository
import kotlin.properties.Delegates

class ForumPagingSource(
    private val forumRepository: ForumRepository,
) : BasePagingSource<ShowF>() {
    private var cuisineId by Delegates.notNull<Long>()

    fun initCuisine(id: Long) {
        cuisineId = id
    }

    override suspend fun fetchData(page: Int, limit: Int): List<ShowF> {
        return when (val a = forumRepository.showf(cuisineId, page.toLong())) {
            is SaniouResponse.Success -> a.data
            is SaniouResponse.Error -> throw a.ex
        }
    }
}
