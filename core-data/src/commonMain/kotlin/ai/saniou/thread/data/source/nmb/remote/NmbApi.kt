package ai.saniou.thread.data.source.nmb.remote

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.thread.data.source.nmb.remote.dto.Forum
import ai.saniou.thread.data.source.nmb.remote.dto.ForumCategory
import ai.saniou.thread.data.source.nmb.remote.dto.Thread
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query

interface NmbApi {
    @GET("getForumList")
    suspend fun getForumList(): SaniouResponse<List<ForumCategory>>

    @GET("showf")
    suspend fun showf(
        @Query("id") id: Long,
        @Query("page") page: Long,
    ): SaniouResponse<List<Forum>>

    @GET("timeline")
    suspend fun timeline(
        @Query("id") id: Long,
        @Query("page") page: Long,
    ): SaniouResponse<List<Forum>>

    @GET("thread")
    suspend fun thread(
        @Query("id") id: Long,
        @Query("page") page: Long,
    ): SaniouResponse<Thread>
}