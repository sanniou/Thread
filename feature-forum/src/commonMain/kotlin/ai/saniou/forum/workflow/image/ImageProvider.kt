package ai.saniou.forum.workflow.image

import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.usecase.thread.FetchTopicImagePageUseCase
import ai.saniou.thread.domain.usecase.thread.GetTopicImagesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * 图片预览数据源：支持一次全量加载，或分页追加（贴吧 picpage）。
 */
interface ImageProvider {
    /** 首次/全量加载。 */
    fun load(): Flow<List<Image>>

    /** 是否支持向后分页追加。 */
    val supportsPaging: Boolean get() = false

    /** 从当前列表末尾继续拉下一批。返回新增图片（空表示到底）。 */
    suspend fun loadMore(current: List<Image>): List<Image> = emptyList()
}

/**
 * 帖内图片：本地缓存优先，Tieba 等源可走 [FetchTopicImagePageUseCase] 远程分页。
 */
class ThreadImageProvider(
    private val sourceId: String,
    private val threadId: String,
    private val channelId: String = "",
    private val channelName: String = "",
    private val getTopicImagesUseCase: GetTopicImagesUseCase,
    private val fetchTopicImagePageUseCase: FetchTopicImagePageUseCase? = null,
) : ImageProvider {

    override val supportsPaging: Boolean =
        fetchTopicImagePageUseCase != null && channelId.isNotBlank()

    override fun load(): Flow<List<Image>> = flow {
        val cached = runCatching {
            getTopicImagesUseCase(sourceId, threadId).first()
        }.getOrDefault(emptyList())

        if (cached.isNotEmpty()) {
            emit(cached)
            return@flow
        }

        if (supportsPaging && fetchTopicImagePageUseCase != null) {
            val remote = fetchTopicImagePageUseCase(
                sourceId = sourceId,
                threadId = threadId,
                channelId = channelId,
                channelName = channelName,
                picId = "",
                picIndex = "1",
                seeLz = false,
                forward = true,
                batchSize = 10,
            ).getOrElse { emptyList() }
            emit(remote)
        } else {
            emit(emptyList())
        }
    }

    override suspend fun loadMore(current: List<Image>): List<Image> {
        val fetch = fetchTopicImagePageUseCase ?: return emptyList()
        if (channelId.isBlank() || current.isEmpty()) return emptyList()
        val last = current.last()
        val picIndex = last.name?.takeIf { it.isNotBlank() } ?: current.size.toString()
        val more = fetch(
            sourceId = sourceId,
            threadId = threadId,
            channelId = channelId,
            channelName = channelName,
            picId = "",
            picIndex = picIndex,
            seeLz = false,
            forward = true,
            batchSize = 10,
        ).getOrElse { emptyList() }
        val seen = current.map { it.originalUrl }.toHashSet()
        return more.filter { it.originalUrl.isNotBlank() && it.originalUrl !in seen }
    }
}
