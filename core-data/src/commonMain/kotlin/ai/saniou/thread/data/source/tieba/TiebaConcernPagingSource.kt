package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.remote.ClientVersion
import ai.saniou.thread.data.source.tieba.remote.OfficialProtobufTiebaApi
import ai.saniou.thread.data.source.tieba.remote.TiebaProtoBuilder
import ai.saniou.thread.domain.model.forum.Topic
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeRequest
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeRequestData
import kotlinx.coroutines.flow.first

class TiebaConcernPagingSource(
    private val api: OfficialProtobufTiebaApi,
    private val parameterProvider: TiebaParameterProvider,
) : PagingSource<Int, Topic>() {
    override fun getRefreshKey(state: PagingState<Int, Topic>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Topic> {
        val page = params.key ?: 1
        return try {
            val request = UserLikeRequest(
                UserLikeRequestData(
                    common = TiebaProtoBuilder.buildCommonRequest(
                        parameterProvider,
                        ClientVersion.TIEBA_V11
                    ),
                    pageTag = if (page == 1) "" else page.toString(),
                    loadType = if (page == 1) 1 else 2
                )
            )

            val body = TiebaProtoBuilder.buildProtobufFormBody(
                data = request,
                clientVersion = ClientVersion.TIEBA_V11,
                parameterProvider = parameterProvider
            )

            val response = api.userLikeFlow(body)

            if (response.error?.error_code != 0) {
                return LoadResult.Error(Exception("Tieba Error: ${response.error?.error_msg} (Code: ${response.error?.error_code})"))
            }

            val topics = TiebaMapper.mapUserLikeResponseToTopics(response)

            val nextKey = if (topics.isNotEmpty()) page + 1 else null

            LoadResult.Page(
                data = topics,
                prevKey = if (page == 1) null else page - 1,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
