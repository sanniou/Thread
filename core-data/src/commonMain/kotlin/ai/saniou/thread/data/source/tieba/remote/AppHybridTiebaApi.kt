package ai.saniou.thread.data.source.tieba.remote

import ai.saniou.thread.data.source.tieba.model.SearchForumBean
import ai.saniou.thread.data.source.tieba.model.SearchThreadBean
import ai.saniou.thread.data.source.tieba.model.SearchUserBean
import ai.saniou.thread.data.source.tieba.model.TopicDetailBean
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.Query

/**
 * 移植自 com.huanchengfly.tieba.post.api.retrofit.interfaces.AppHybridTiebaApi
 * 包含混合开发相关的搜索接口
 */
interface AppHybridTiebaApi {
    /**
     * 搜索吧 (混合开发接口)
     *
     * @param keyword 搜索关键词
     * @param referer 来源页面 URL (通常需要 UrlEncode)
     * @return 搜索到的吧列表结果
     */
    @Headers(
        "No-St-Params: 1",
        "No-Common-Params: BDUSS,STOKEN"
    )
    @GET("mo/q/search/forum")
    suspend fun searchForum(
        @Query("word") keyword: String,
        @Header("Referer") referer: String
    ): SearchForumBean

    /**
     * 搜索帖子 (混合开发接口)
     *
     * @param keyword 搜索关键词
     * @param page 页码
     * @param sort 排序方式
     * @param filter 过滤器 (1: 全部, 2: 精品)
     * @param pageSize 每页数量
     * @param forumName 吧名 (可选，在特定吧内搜索)
     * @param ct 内容类型 (1: 帖子)
     * @param isUseZonghe 是否使用综合搜索
     * @param clientVersion 客户端版本号
     * @param referer 来源页面 URL
     * @return 搜索到的帖子列表结果
     */
    @Headers(
        "No-St-Params: 1",
        "No-Common-Params: BDUSS,STOKEN"
    )
    @GET("mo/q/search/thread")
    suspend fun searchThread(
        @Query("word") keyword: String,
        @Query("pn") page: Int,
        @Query("st") sort: Int,
        @Query("tt") filter: Int = 1,
        @Query("rn") pageSize: Int? = null,
        @Query("fname") forumName: String? = null,
        @Query("ct") ct: Int = 1,
        @Query("is_use_zonghe") isUseZonghe: Int? = 1,
        @Query("cv") clientVersion: String = "99.9.101",
        @Header("Referer") referer: String
    ): SearchThreadBean

    /**
     * 搜索用户 (混合开发接口)
     *
     * @param keyword 搜索关键词
     * @param referer 来源页面 URL
     * @return 搜索到的用户列表结果
     */
    @Headers(
        "No-St-Params: 1",
        "No-Common-Params: BDUSS,STOKEN"
    )
    @GET("mo/q/search/user")
    suspend fun searchUser(
        @Query("word") keyword: String,
        @Header("Referer") referer: String
    ): SearchUserBean

    /**
     * 获取话题详情 (混合开发接口)
     *
     * @param topicId 话题 ID
     * @param topicName 话题名称
     * @param isNew 是否为新话题
     * @param isShare 是否分享
     * @param page 页码
     * @param pageSize 每页数量
     * @param offset 偏移量
     * @param derivativeToPicId 衍生图片 ID
     * @return 话题详情结果
     */
    @GET("mo/q/newtopic/topicDetail")
    suspend fun topicDetail(
        @Query("topic_id") topicId: String,
        @Query("topic_name") topicName: String,
        @Query("is_new") isNew: Int = 0,
        @Query("is_share") isShare: Int = 1,
        @Query("pn") page: Int,
        @Query("rn") pageSize: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("derivative_to_pic_id") derivativeToPicId: String = ""
    ): TopicDetailBean
}
