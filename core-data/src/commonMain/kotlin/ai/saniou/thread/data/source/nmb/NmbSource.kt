package ai.saniou.thread.data.source.nmb

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.thread.data.source.nmb.remote.NmbApi
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.repository.Source

class NmbSource(private val nmbApi: NmbApi) : Source {
    override val id: String = "nmb"

    override suspend fun getForums(): Result<List<Forum>> {
        return when (val response = nmbApi.getForumList()) {
            is SaniouResponse.Success -> {
                val forums = response.data.flatMap { it.forums }.map { it.toDomain() }
                Result.success(forums)
            }
            is SaniouResponse.Error -> Result.failure(response.ex)
        }
    }

    override suspend fun getPosts(forumId: String, page: Int): Result<List<Post>> {
        return when (val response = nmbApi.showf(forumId.toLong(), page.toLong())) {
            is SaniouResponse.Success -> {
                val posts = response.data.map { it.toDomain() }
                Result.success(posts)
            }
            is SaniouResponse.Error -> Result.failure(response.ex)
        }
    }
}