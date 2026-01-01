package ai.saniou.thread.data.source.tieba.remote

import ai.saniou.thread.data.source.tieba.model.CommonResponse
import ai.saniou.thread.data.source.tieba.model.MessageListBean
import ai.saniou.thread.data.source.tieba.model.MsgBean
import ai.saniou.thread.data.source.tieba.model.ThreadStoreBean
import ai.saniou.thread.network.tieba.TiebaApiConstants
import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST

/**
 * 移植自 com.huanchengfly.tieba.post.api.retrofit.interfaces.NewTiebaApi
 * 包含消息、收藏、回复等相关接口
 */
interface NewTiebaApi {
    /**
     * 获取消息列表（回复、@、点赞等）
     * 对应原接口: /c/s/msg
     *
     * @param bookmark 书签
     * @return 消息列表数据
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/s/msg")
    @FormUrlEncoded
    suspend fun msg(
        @Field("bookmark") bookmark: Int = 1
    ): MsgBean

    /**
     * 获取收藏帖子列表
     * 对应原接口: /c/f/post/threadstore
     *
     * @param pageSize 每页数量
     * @param offset 偏移量
     * @param user_id 用户 ID
     * @return 收藏帖子列表数据
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/f/post/threadstore")
    @FormUrlEncoded
    suspend fun threadStore(
        @Field("rn") pageSize: Int,
        @Field("offset") offset: Int,
        @Field("user_id") user_id: String?
    ): ThreadStoreBean

    /**
     * 取消收藏帖子
     * 对应原接口: /c/c/post/rmstore
     *
     * @param threadId 帖子 ID
     * @param tbs TBS Token
     * @return 操作结果
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/c/post/rmstore")
    @FormUrlEncoded
    suspend fun removeStore(
        @Field("tid") threadId: String,
        @Field("tbs") tbs: String
    ): CommonResponse


    /**
     * 添加收藏帖子
     * 对应原接口: /c/c/post/addstore
     *
     * @param data 收藏数据
     * @param tbs TBS Token
     * @return 操作结果
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/c/post/addstore")
    @FormUrlEncoded
    suspend fun addStore(
        @Field("data") data: String,
        @Field("tbs") tbs: String
    ): CommonResponse


    /**
     * 获取回复我的消息列表
     * 对应原接口: /c/u/feed/replyme
     *
     * @param page 页码
     * @return 消息列表数据
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/u/feed/replyme")
    @FormUrlEncoded
    suspend fun replyMe(
        @Field("pn") page: Int = 0
    ): MessageListBean


    /**
     * 获取@我的消息列表
     * 对应原接口: /c/u/feed/atme
     *
     * @param page 页码
     * @return 消息列表数据
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/u/feed/atme")
    @FormUrlEncoded
    suspend fun atMe(
        @Field("pn") page: Int = 0
    ): MessageListBean

    /**
     * 获取点赞我的消息列表
     * 对应原接口: /c/u/feed/agreeme
     *
     * @param page 页码
     * @return 消息列表数据
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/u/feed/agreeme")
    @FormUrlEncoded
    suspend fun agreeMe(
        @Field("pn") page: Int = 0
    ): MessageListBean
}
