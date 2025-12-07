package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.network.SaniouResponse

class NmbSource(private val nmbXdApi: NmbXdApi) : Source {
    override val id: String = "nmb"

    override suspend fun getForums(): Result<List<Forum>> {
        return when (val response = nmbXdApi.getForumList()) {
            is SaniouResponse.Success -> {
                val forums = response.data.flatMap { it.forums }.map { it.toDomain() }
                Result.success(forums)
            }

            is SaniouResponse.Error -> Result.failure(response.ex)
        }
    }

    override suspend fun getPosts(forumId: String, page: Int): Result<List<Post>> {
        TODO()
    }
}
