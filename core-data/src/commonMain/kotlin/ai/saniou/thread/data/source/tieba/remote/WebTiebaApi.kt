package ai.saniou.thread.data.source.tieba.remote

import ai.saniou.thread.data.source.tieba.model.CommonResponse
import ai.saniou.thread.data.source.tieba.model.ForumHome
import ai.saniou.thread.data.source.tieba.model.ForumPageBean
import ai.saniou.thread.data.source.tieba.model.HotMessageListBean
import ai.saniou.thread.data.source.tieba.model.HotTopicBean
import ai.saniou.thread.data.source.tieba.model.HotTopicForumBean
import ai.saniou.thread.data.source.tieba.model.HotTopicMainBean
import ai.saniou.thread.data.source.tieba.model.HotTopicThreadBean
import ai.saniou.thread.data.source.tieba.model.MyInfoBean
import ai.saniou.thread.data.source.tieba.model.SearchForumBean
import ai.saniou.thread.data.source.tieba.model.SearchThreadBean
import ai.saniou.thread.data.source.tieba.model.WebProfile
import ai.saniou.thread.data.source.tieba.model.WebReplyResultBean
import ai.saniou.thread.data.source.tieba.model.WebUploadPicBean
import ai.saniou.thread.network.tieba.TiebaApiConstants
import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Query
import de.jensklingenberg.ktorfit.http.Url
import kotlin.time.Clock

/**
 * 移植自 com.huanchengfly.tieba.post.api.retrofit.interfaces.WebTiebaApi
 */
interface WebTiebaApi {
    /**
     * 关注吧 (Web接口)
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @GET
    suspend fun follow(
        @Url url: String
    ): CommonResponse

    /**
     * 取消关注吧 (Web接口)
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @GET
    suspend fun unfollow(
        @Url url: String
    ): CommonResponse

    /**
     * 获取吧首页信息 (Web接口)
     * 对应原接口: /mg/o/getForumHome
     */
    @Headers(
        "${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}",
        "Referer: https://tieba.baidu.com/index/tbwise/forum?source=index",
        "sec-ch-ua: \\\".Not/A)Brand\\\";v=\\\"99\\\", \\\"Microsoft Edge\\\";v=\\\"103\\\", \\\"Chromium\\\";v=\\\"103\\\"",
        "sec-ch-ua-mobile: ?1",
        "sec-ch-ua-platform: Android"
    )
    @GET("/mg/o/getForumHome")
    suspend fun getForumHome(
        @Query("st") sortType: Int,
        @Query("pn") page: Int,
        @Query("rn") pageSize: Int,
        @Query("eqid") eqid: String,
        @Query("refer") refer: String
    ): ForumHome

    /**
     * 获取个人资料 (Web接口)
     * 对应原接口: /mg/o/profile
     */
    @Headers(
        "${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}",
        "Referer: https://tieba.baidu.com/index/tbwise/mine?source=index",
        "sec-ch-ua: \\\".Not/A)Brand\\\";v=\\\"99\\\", \\\"Microsoft Edge\\\";v=\\\"103\\\", \\\"Chromium\\\";v=\\\"103\\\"",
        "sec-ch-ua-mobile: ?1",
        "sec-ch-ua-platform: Android"
    )
    @GET("/mg/o/profile")
    suspend fun myProfile(
        @Query("format") format: String,
        @Query("eqid") eqid: String,
        @Query("refer") refer: String
    ): WebProfile

    /**
     * 热门话题 (主页)
     * 对应原接口: /mo/q/hotMessage/main
     */
    @GET("/mo/q/hotMessage/main")
    suspend fun hotTopicMain(
        @Query("topic_id") topicId: String,
        @Query("yuren_rand") yurenRand: String,
        @Query("topic_name") topicName: String,
        @Query("pmy_topic_ext") pmyTopicExt: String
    ): HotTopicMainBean

    /**
     * 热门话题 (吧内)
     * 对应原接口: /mo/q/hotMessage/forum
     */
    @GET("/mo/q/hotMessage/forum")
    suspend fun hotTopicForum(
        @Query("topic_id") topicId: String,
        @Query("yuren_rand") yurenRand: String,
        @Query("topic_name") topicName: String,
        @Query("pmy_topic_ext") pmyTopicExt: String
    ): HotTopicForumBean

    /**
     * 热门话题 (帖子)
     * 对应原接口: /mo/q/hotMessage/thread
     */
    @GET("/mo/q/hotMessage/thread")
    suspend fun hotTopicThread(
        @Query("topic_id") topicId: String,
        @Query("yuren_rand") yurenRand: String,
        @Query("topic_name") topicName: String,
        @Query("pmy_topic_ext") pmyTopicExt: String,
        @Query("page") page: Int,
        @Query("num") num: Int = 30,
        @Query("forum_id") forum_id: String = ""
    ): HotTopicThreadBean

    /**
     * 热门话题 (通用)
     * 对应原接口: /mo/q/hotMessage
     */
    @GET("/mo/q/hotMessage")
    suspend fun hotTopic(
        @Query("topic_id") topicId: String,
        @Query("topic_name") topicName: String,
        @Query("fr") fr: String = "newwise"
    ): HotTopicBean

    /**
     * 热门话题列表
     * 对应原接口: /mo/q/hotMessage/list
     */
    @GET("/mo/q/hotMessage/list?fr=newwise")
    suspend fun hotMessageList(): HotMessageListBean

    /**
     * 获取吧帖子列表 (Web接口)
     * 对应原接口: /f
     */
    @GET("/f")
    suspend fun frs(
        @Query("kw") forumName: String,
        @Query("pn") pn: Int,
        @Query("sort_type") sort_type: Int,
        @Query("cid") cid: String?,
        @Query("lm") lm: String? = null,
        @Query("fr") fr: String = "newwise"
    ): ForumPageBean

    /**
     * 获取我的信息 (Web接口，需Cookie)
     * 对应原接口: /mo/q/newmoindex
     */
    @Headers("Add-Web-Cookie: 0")
    @GET("/mo/q/newmoindex?need_user=1")
    suspend fun myInfo(
        @Header("cookie") cookie: String
    ): MyInfoBean

    /**
     * 搜索吧
     * 对应原接口: /mo/q/search/forum
     */
    @GET("/mo/q/search/forum")
    suspend fun searchForum(
        @Query("word") keyword: String
    ): SearchForumBean

    /**
     * 搜索帖子
     * 对应原接口: /mo/q/search/thread
     */
    @GET("/mo/q/search/thread")
    suspend fun searchThread(
        @Query("word") keyword: String,
        @Query("pn") page: Int,
        @Query("st") order: String,
        @Query("tt") filter: String,
        @Query("ct") ct: String = "2"
    ): SearchThreadBean

    /**
     * Web端上传图片
     * 对应原接口: /mo/q/cooluploadpic
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("/mo/q/cooluploadpic")
    @FormUrlEncoded
    suspend fun webUploadPic(
        @Field("pic") base64: String?,
        @Query("type") type: String = "ajax",
        @Query("r") r: String = ""
    ): WebUploadPicBean

    /**
     * Web端回复帖子
     * 对应原接口: /mo/q/apubpost
     */
    @Headers(
        "Host: tieba.baidu.com",
        "Origin: https://tieba.baidu.com",
        "X-Requested-With: XMLHttpRequest"
    )
    @POST("/mo/q/apubpost")
    @FormUrlEncoded
    suspend fun webReply(
        @Query("_t") _t_url: Long = Clock.System.now().toEpochMilliseconds(),
        @Field("co") content: String,
        @Field("_t") _t_form: Long = Clock.System.now().toEpochMilliseconds(),
        @Field("tag") tag: String = "11",
        @Field("upload_img_info") imgInfo: String,
        @Field("fid") forumId: String,
        @Field("src") src: String = "1",
        @Field("word") forumName: String,
        @Field("tbs") tbs: String,
        @Field("z") threadId: String,
        @Field("lp") lp: String = "6026",
        @Field("nick_name") nickName: String,
        @Field("pid") postId: String? = null,
        @Field("lzl_id") replyPostId: String? = null,
        @Field("floor") floor: String? = null,
        @Field("_BSK") bsk: String,
        @Header("Referer") referer: String
    ): WebReplyResultBean
}
