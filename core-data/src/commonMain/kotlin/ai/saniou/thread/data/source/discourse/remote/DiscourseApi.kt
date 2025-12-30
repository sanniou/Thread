package ai.saniou.thread.data.source.discourse.remote

import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseCategoriesResponse
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseLatestPostsResponse
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopicDetailResponse
import ai.saniou.thread.network.SaniouResult
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query

interface DiscourseApi {

    /**
     * Get latest topics
     * https://docs.discourse.org/#tag/Topics/operation/listLatestTopics
     */
    @GET("latest.json")
    suspend fun getLatestTopics(
        @Query("page") page: Int = 0,
    ): SaniouResult<DiscourseLatestPostsResponse>

    /**
     * Get categories
     * https://docs.discourse.org/#tag/Categories/operation/listCategories
     */
    @GET("categories.json")
    suspend fun getCategories(
        @Query("include_subcategories") includeSubcategories: Boolean = true,
    ): SaniouResult<DiscourseCategoriesResponse>

    /**
     * Get topics in a specific category
     * https://docs.discourse.org/#tag/Categories/operation/listCategoryTopics
     */
    @GET("c/{id}.json")
    suspend fun getCategoryTopics(
        @Path("id") id: String,
        @Query("page") page: Int = 0,
    ): SaniouResult<DiscourseLatestPostsResponse>

    /**
     * Get topic detail
     * https://docs.discourse.org/#tag/Topics/operation/getTopic
     */
    @GET("t/{id}.json")
    suspend fun getTopic(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
    ): SaniouResult<DiscourseTopicDetailResponse>
}
