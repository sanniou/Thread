package ai.saniou.nmb.domain

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.ForumCategory
import ai.saniou.nmb.data.entity.ForumDetail
import ai.saniou.nmb.data.repository.ForumRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.collections.filter

class ForumCategoryUseCase(
    private val forumRepository: ForumRepository
) {
    suspend operator fun invoke(): List<ForumCategory> = coroutineScope {

        val forumList = async {
            when (val forumList = forumRepository.getForumList()) {
                is SaniouResponse.Success -> forumList.data.apply {
                    forEach { forumCategory ->
                        forumCategory.forums = forumCategory.forums.filter { forumDetail ->
                            forumDetail.id > 0
                        }
                    }
                }

                is SaniouResponse.Error -> throw forumList.ex
            }
        }
        val timeLineList = async {
            when (val forumList = forumRepository.getTimelineList()) {
                is SaniouResponse.Success -> forumList.data
                is SaniouResponse.Error -> throw forumList.ex
            }

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
}
