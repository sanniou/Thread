package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.remote.ClientVersion
import ai.saniou.thread.data.source.tieba.remote.OfficialProtobufTiebaApi
import ai.saniou.thread.data.source.tieba.remote.TiebaProtoBuilder
import ai.saniou.thread.domain.model.forum.Topic
import app.cash.paging.PagingSource
import app.cash.paging.PagingState
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedRequest
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedRequestData
import kotlinx.coroutines.flow.first

class TiebaRecommendPagingSource(
    private val api: OfficialProtobufTiebaApi,
    private val parameterProvider: TiebaParameterProvider,
) : PagingSource<Int, Topic>() {
    override fun getRefreshKey(state: PagingState<Int, Topic>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Topic> {
        val page = params.key ?: 1
        return try {
            val request = PersonalizedRequest(
                PersonalizedRequestData(
                    common = TiebaProtoBuilder.buildCommonRequest(
                        parameterProvider,
                        ClientVersion.TIEBA_V11
                    ),
                    load_type = if (page == 1) 1 else 2,
                    pn = page
                )
            )

            val body = TiebaProtoBuilder.buildProtobufFormBody(
                data = request,
                clientVersion = ClientVersion.TIEBA_V11,
                parameterProvider = parameterProvider
            )

            val response = api.personalizedFlow(body).first()

            if (response.error?.error_code != 0) {
                return LoadResult.Error(Exception("Tieba Error: ${response.error?.error_msg} (Code: ${response.error?.error_code})"))
            }

            val topics = TiebaMapper.mapPersonalizedResponseToTopics(response)

            val nextKey = page + 1

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
