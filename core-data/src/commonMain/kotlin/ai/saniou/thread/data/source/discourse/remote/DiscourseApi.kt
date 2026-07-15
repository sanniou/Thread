package ai.saniou.thread.data.source.discourse.remote

import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseCategoriesResponse
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseLatestPostsResponse
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopicDetailResponse
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseCreatePostResponse
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseSearchResponse
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseUserActionsResponse
import ai.saniou.thread.network.SaniouResult
import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST
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

    @GET("search.json")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
    ): SaniouResult<DiscourseSearchResponse>

    @GET("user_actions.json")
    suspend fun getUserActions(
        @Query("username") username: String,
        @Query("filter") filter: Int,
        @Query("offset") offset: Int = 0,
    ): SaniouResult<DiscourseUserActionsResponse>

    @POST("posts.json")
    @FormUrlEncoded
    suspend fun createPost(
        @Field("raw") raw: String,
        @Field("title") title: String? = null,
        @Field("category") category: String? = null,
        @Field("topic_id") topicId: String? = null,
    ): SaniouResult<DiscourseCreatePostResponse>
}
