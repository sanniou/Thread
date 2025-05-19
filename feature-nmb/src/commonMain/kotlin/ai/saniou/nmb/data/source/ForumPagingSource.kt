package ai.saniou.nmb.data.source

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.ShowF
import ai.saniou.nmb.data.repository.ForumRepository
import kotlin.properties.Delegates

class ForumPagingSource(
    private val forumRepository: ForumRepository,
) : BasePagingSource<ShowF>() {
    private var cuisineId by Delegates.notNull<Long>()
    private var fgroup by Delegates.notNull<Long>()

    fun initCuisine(id: Long, fgroup: Long) {
        cuisineId = id
        this.fgroup = fgroup
    }

    override suspend fun fetchData(page: Int, limit: Int): List<ShowF> {
        return when (val a = threadList(page, limit)) {
            is SaniouResponse.Success -> a.data
            is SaniouResponse.Error -> throw a.ex
        }
    }

    private suspend fun threadList(
        page: Int,
        limit: Int
    ): SaniouResponse<List<ShowF>> =
        if (fgroup == -1L)
            forumRepository.timeline(cuisineId, page.toLong())
        else
            forumRepository.showf(cuisineId, page.toLong())
}
