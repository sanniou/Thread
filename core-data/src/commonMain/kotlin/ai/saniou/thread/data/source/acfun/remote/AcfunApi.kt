package ai.saniou.thread.data.source.acfun.remote

import ai.saniou.thread.data.source.acfun.remote.dto.AcfunArticleResponse
import ai.saniou.thread.data.source.acfun.remote.dto.AcfunCommentListResponse
import ai.saniou.thread.data.source.acfun.remote.dto.AcfunRankResponse
import ai.saniou.thread.data.source.acfun.remote.dto.AcfunVisitorLoginResponse
import ai.saniou.thread.network.SaniouResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Query

/**
 * AcFun 移动端 API
 *
 * 基础配置:
 * - User-Agent: acvideo core/6.31.1.1026(OPPO;OPPO A83;7.1.1)
 * - deviceType: 1
 * - productId: 2000
 * - market: tencent
 *
 * 域名说明:
 * - 登录: id.app.acfun.cn
 * - 核心业务: api-new.app.acfun.cn
 * - 评论/搜索/番剧: api-ipv6.app.acfun.cn (需配合特定 appVersion)
 */
interface AcfunApi {

    // ============================================================================================
    // 鉴权与 Token (Authentication)
    // ============================================================================================

    /**
     * 游客登录
     *
     * @param sid 固定为 "acfun.api.visitor"
     */
    @POST("https://id.app.acfun.cn/rest/app/visitor/login")
    @FormUrlEncoded
    suspend fun visitorLogin(
        @Field("sid") sid: String = "acfun.api.visitor"
    ): SaniouResponse<AcfunVisitorLoginResponse>

    /**
     * 账号登录
     *
     * @param username 用户名/手机号
     * @param password 密码
     * @return 包含 acPassToken, auth_key, token, acSecurity, userid 等信息
     */
    @POST("https://id.app.acfun.cn/rest/app/login/signin")
    @FormUrlEncoded
    suspend fun login(
        @Field("username") user: String,
        @Field("password") psw: String
    ): SaniouResponse<Any> // TODO: 定义 LoginResponse

    /**
     * 获取中台 Token (Midground Token)
     *
     * @param sid 固定为 "acfun.midground.api"
     * @return 包含 acfun.midground.api_st
     */
    @POST("https://id.app.acfun.cn/rest/app/token/get")
    @FormUrlEncoded
    suspend fun getToken(
        @Field("sid") sid: String = "acfun.midground.api"
    ): SaniouResponse<Any> // TODO: 定义 TokenResponse

    /**
     * 检查是否已签到
     */
    @POST("https://api-ipv6.app.acfun.cn/rest/app/user/hasSignedIn")
    @FormUrlEncoded
    suspend fun hasSignedIn(
        @Field("access_token") accessToken: String
    ): SaniouResponse<Any> // TODO: 定义 HasSignedInResponse

    /**
     * 执行签到
     */
    @POST("https://api-ipv6.app.acfun.cn/rest/app/user/signIn")
    @FormUrlEncoded
    suspend fun signIn(
        @Field("access_token") accessToken: String
    ): SaniouResponse<Any>

    // ============================================================================================
    // 用户信息 (User Profile)
    // ============================================================================================

    /**
     * 获取当前登录用户的个人信息
     */
    @GET("https://api-ipv6.app.acfun.cn/rest/app/user/personalInfo")
    suspend fun getPersonalInfo(): SaniouResponse<Any> // TODO: 定义 UserInfo

    /**
     * 获取指定用户信息
     */
    @GET("https://api-new.app.acfun.cn/rest/app/user/userInfo")
    suspend fun getUserInfo(
        @Query("userId") userId: Long
    ): SaniouResponse<Any>

    /**
     * 检查是否关注了指定用户
     */
    @GET("https://api-new.app.acfun.cn/rest/app/relation/isFollowing")
    suspend fun isFollowing(
        @Query("toUserIds") toUserIds: Long
    ): SaniouResponse<Any>

    /**
     * 关注/取消关注用户
     *
     * @param action 1: 关注, 2: 取消关注
     * @param toUserId 目标用户 ID
     */
    @POST("https://api-new.app.acfun.cn/rest/app/relation/follow")
    @FormUrlEncoded
    suspend fun follow(
        @Field("action") action: Int,
        @Field("toUserId") toUserId: Long,
        @Field("groupId") groupId: Int = 0
    ): SaniouResponse<Any>

    /**
     * 获取运营位列表 (Operation List)
     */
    @POST("https://api-new.app.acfun.cn/rest/app/operation/getOperations")
    @FormUrlEncoded
    suspend fun getOperationList(
        @Field("pcursor") pcursor: String = "",
        @Field("limit") limit: Int = 10
    ): SaniouResponse<Any>

    /**
     * 获取用户动态列表
     */
    @GET("https://api-new.app.acfun.cn/rest/app/feed/profile")
    suspend fun getUserProfile(
        @Query("userId") userId: Long,
        @Query("pcursor") pcursor: String,
        @Query("count") count: Int = 10
    ): SaniouResponse<Any>

    /**
     * 获取用户投稿资源 (视频/文章)
     *
     * @param resourceType 2: 视频, 3: 文章
     * @param sortType 3: 最新, 2: 香蕉最多, 1: 播放最多
     * @param status 1: 正常?
     */
    @POST("https://api-new.app.acfun.cn/rest/app/user/resource/query")
    @FormUrlEncoded
    suspend fun getUserResource(
        @Field("authorId") authorId: Long,
        @Field("resourceType") resourceType: Int,
        @Field("pcursor") pcursor: String,
        @Field("count") count: Int = 10,
        @Field("sortType") sortType: Int = 3,
        @Field("status") status: Int = 1
    ): SaniouResponse<Any>

    // ============================================================================================
    // 内容获取 (Content: Video, Article, Channel)
    // ============================================================================================

    /**
     * 获取频道排行列表
     */
    @GET("https://api-new.app.acfun.cn/rest/app/rank/getChannelList")
    suspend fun getRankChannelList(): SaniouResponse<Any>

    /**
     * 获取文章热门榜
     */
    @GET("https://api-ipv6.app.acfun.cn/rest/app/rank/channel")
    suspend fun getArticleHotRank(
        @Query("rankPeriod") rankPeriod: String = "THREE_DAYS",
        @Query("channelId") channelId: Int = 63
    ): SaniouResponse<AcfunRankResponse>

    /**
     * 获取视频播放信息 (核心接口)
     *
     * @param videoId 视频 ID
     * @param resourceId 资源 ID (通常是 ac 号对应的数字)
     * @param resourceType 资源类型
     * @param mkey 魔法密钥 (c_mkey)
     */
    @GET("https://api-new.app.acfun.cn/rest/app/play/playInfo/cast")
    suspend fun getVideoPlayInfo(
        @Query("videoId") videoId: Long,
        @Query("resourceId") resourceId: Long,
        @Query("resourceType") resourceType: Int,
        @Query("mkey") mkey: String = "AAHewK3eIAAyMjA2MDMyMjQAAhAAMEP1uwSG3TvhYAAAAO5fOOpIdKsH2h4IGsF6BlVwnGQA6_eLEvGiajzUp4_YthxOPC-hxcOpTk0SPSrxyhbdkmIwsXnF9PgS5ly8eQyjuXlcS7VpWG0QlK0HakVDamteMHNHIui0A8V4tmELqQ%3D%3D"
    ): SaniouResponse<Any>

    /**
     * 获取文章详情
     */
    @GET("https://api-new.app.acfun.cn/rest/app/article/info")
    suspend fun getArticleInfo(
        @Query("articleId") articleId: Long
    ): SaniouResponse<AcfunArticleResponse>

    /**
     * 获取推荐视频列表 (相关推荐)
     */
    @GET("https://api-new.app.acfun.cn/rest/app/feed/related/general")
    suspend fun getRelatedFeedList(
        @Query("resourceId") resourceId: Long,
        @Query("resourceType") resourceType: Int,
        @Query("count") count: Int,
        @Query("appMode") appMode: String = "0"
    ): SaniouResponse<Any>

    /**
     * 上报播放内容
     * 注意: Content-Type 为 application/x-protobuf
     */
    @POST("https://apilog.app.acfun.cn/rest/app/report/playContent")
    @Headers("Content-Type: application/x-protobuf")
    suspend fun reportPlayContent(
        @Body body: ByteArray
    ): SaniouResponse<Any>

    // ============================================================================================
    // 评论与互动 (Comments & Interaction)
    // ============================================================================================

    /**
     * 获取评论列表
     *
     * 注意: 需使用 api-ipv6.app.acfun.cn 域名和特定 appVersion
     *
     * @param sourceType 1: 文章, 2: 番剧, 3: 视频, 4: 动态
     * @param showHotComments 首页传 1，翻页传 0
     */
    @GET("https://api-ipv6.app.acfun.cn/rest/app/comment/list")
    @Headers("appVersion: 6.31.1.1026")
    suspend fun getCommentList(
        @Query("sourceId") sourceId: Long,
        @Query("sourceType") sourceType: Int,
        @Query("pcursor") pcursor: String,
        @Query("count") count: Int = 20,
        @Query("showHotComments") showHotComments: Int
    ): SaniouResponse<AcfunCommentListResponse>

    /**
     * 获取子评论 (楼中楼)
     *
     * 注意: 需使用 api-ipv6.app.acfun.cn 域名和特定 appVersion
     */
    @GET("https://api-ipv6.app.acfun.cn/rest/app/comment/sublist")
    @Headers("appVersion: 6.31.1.1026")
    suspend fun getSubCommentList(
        @Query("sourceId") sourceId: Long,
        @Query("sourceType") sourceType: Int,
        @Query("rootCommentId") rootCommentId: Long,
        @Query("pcursor") pcursor: String,
        @Query("count") count: Int = 20
    ): SaniouResponse<Any>

    /**
     * 获取 PC 端评论列表 (备用)
     */
    @GET("https://www.acfun.cn/rest/pc-direct/comment/list")
    suspend fun getCommentListPC(
        @Query("sourceId") sourceId: Long,
        @Query("sourceType") sourceType: Int = 3,
        @Query("page") page: Int = 1,
        @Query("t") t: Long,
        @Query("showHotComments") showHotComments: Int = 1,
        @Query("supportZtEmot") supportZtEmot: Boolean = true
    ): SaniouResponse<Any>

    /**
     * 发送评论
     *
     * @param content 内容需 encodeURIComponent (Ktorfit 可能自动处理，需验证)
     */
    @POST("https://api-new.app.acfun.cn/rest/app/comment/add")
    @FormUrlEncoded
    suspend fun addComment(
        @Field("sourceId") sourceId: Long,
        @Field("sourceType") sourceType: Int,
        @Field("content") content: String,
        @Field("access_token") accessToken: String,
        @Field("replyToCommentId") replyToCommentId: Long? = null,
        @Field("syncToMoment") syncToMoment: Int = 0
    ): SaniouResponse<Any>

    /**
     * 点赞/取消点赞评论
     */
    @POST("https://api-new.app.acfun.cn/rest/app/comment/like")
    @FormUrlEncoded
    suspend fun likeComment(
        @Field("sourceId") sourceId: Long,
        @Field("sourceType") sourceType: Int,
        @Field("commentId") commentId: Long
    ): SaniouResponse<Any>

    @POST("https://api-new.app.acfun.cn/rest/app/comment/unlike")
    @FormUrlEncoded
    suspend fun unlikeComment(
        @Field("sourceId") sourceId: Long,
        @Field("sourceType") sourceType: Int,
        @Field("commentId") commentId: Long
    ): SaniouResponse<Any>

    /**
     * 收藏资源
     */
    @POST("https://api-new.app.acfun.cn/rest/app/favorite")
    @FormUrlEncoded
    suspend fun addFavorite(
        @Field("resourceId") resourceId: Long,
        @Field("resourceType") resourceType: Int
    ): SaniouResponse<Any>

    /**
     * 取消收藏
     */
    @POST("https://api-new.app.acfun.cn/rest/app/unFavorite")
    @FormUrlEncoded
    suspend fun removeFavorite(
        @Field("resourceIds") resourceIds: Long, // 注意参数名是复数
        @Field("resourceType") resourceType: Int
    ): SaniouResponse<Any>

    /**
     * 投蕉
     */
    @POST("https://api-new.app.acfun.cn/rest/app/banana/throwBanana")
    @FormUrlEncoded
    suspend fun throwBanana(
        @Field("resourceId") resourceId: Long,
        @Field("resourceType") resourceType: Int,
        @Field("count") count: Int
    ): SaniouResponse<Any>

    // ============================================================================================
    // 番剧 (Bangumi)
    // ============================================================================================

    /**
     * 番剧首页
     * 注意: appVersion 需为 6.39.0.1095
     */
    @POST("https://api-ipv6.app.acfun.cn/rest/app/speedTheater")
    @Headers("appVersion: 6.39.0.1095")
    @FormUrlEncoded
    suspend fun getBangumiMainPage(
        @Field("appMode") appMode: String = "0"
    ): SaniouResponse<Any>

    /**
     * 番剧详情
     */
    @POST("https://api-ipv6.app.acfun.cn/rest/app/new-bangumi/detail")
    @FormUrlEncoded
    suspend fun getBangumiDetail(
        @Field("bangumiId") bangumiId: Long
    ): SaniouResponse<Any>

    /**
     * 番剧剧集列表
     */
    @POST("https://api-ipv6.app.acfun.cn/rest/app/new-bangumi/itemList")
    @FormUrlEncoded
    suspend fun getBangumiItemList(
        @Field("bangumiId") bangumiId: Long,
        @Field("pageNo") pageNo: Int = 1,
        @Field("pageSize") pageSize: Int = 1000
    ): SaniouResponse<Any>

    /**
     * 追番 (收藏番剧)
     */
    @GET("https://api-new.app.acfun.cn/rest/app/feed/favorite/bangumi")
    suspend fun getFavoriteBangumiList(
        @Query("pcursor") pcursor: String,
        @Query("count") count: Int,
        @Query("access_token") accessToken: String,
        @Query("appMode") appMode: String = "0"
    ): SaniouResponse<Any>

    /**
     * 检查资源类型
     */
    @GET("https://api-new.app.acfun.cn/rest/app/resource/type")
    suspend fun checkResourceType(
        @Query("resourceCode") acId: String
    ): SaniouResponse<Any>

    // ============================================================================================
    // 搜索与工具 (Search & Utils)
    // ============================================================================================

    /**
     * 搜索推荐
     */
    @GET("https://api-ipv6.app.acfun.cn/rest/app/search/recommend")
    @Headers("appVersion: 6.31.1.1026")
    suspend fun getSearchRecommend(): SaniouResponse<Any>

    /**
     * 综合搜索
     */
    @GET("https://api-ipv6.app.acfun.cn/rest/app/search/complex")
    suspend fun searchComplex(
        @Query("keyword") keyword: String,
        @Query("pCursor") pCursor: String,
        @Query("requestId") requestId: String = "",
        @Query("mkey") mkey: String = "AAHewK3eIAAyMjA2MDMyMjQAAhAAMEP1uwSG3TvhYAAAAO5fOOpIdKsH2h4IGsF6BlVwnGQA6_eLEvGiajzUp4_YthxOPC-hxcOpTk0SPSrxyhbdkmIwsXnF9PgS5ly8eQyjuXlcS7VpWG0QlK0HakVDamteMHNHIui0A8V4tmELqQ%3D%3D"
    ): SaniouResponse<Any>

    /**
     * 获取通知列表
     *
     * @param type 1: 站内公告, 2: 回复, 3: 赞, 4: 艾特, 5: 收藏+投蕉, 8: 礼物, 9: 系统通知
     */
    @GET("https://api-new.app.acfun.cn/rest/app/notify/load")
    @Headers("appVersion: 6.31.1.1026")
    suspend fun getNotifyList(
        @Query("type") type: Int,
        @Query("pCursor") pCursor: String,
        @Query("appMode") appMode: String = "0"
    ): SaniouResponse<Any>

    /**
     * 获取未读消息数量 (Clock)
     */
    @GET("https://api-new.app.acfun.cn/rest/app/clock/r")
    suspend fun getUnreadCount(
        @Query("access_token") accessToken: String
    ): SaniouResponse<Any>

    // ============================================================================================
    // 收藏与历史 (Favorites & History)
    // ============================================================================================

    /**
     * 收藏的视频列表
     */
    @POST("https://api-new.app.acfun.cn/rest/app/favorite/dougaList")
    @FormUrlEncoded
    suspend fun getFavoriteVideoList(
        @Field("cursor") cursor: String,
        @Field("count") count: Int
    ): SaniouResponse<Any>

    /**
     * 收藏的文章列表
     */
    @POST("https://api-new.app.acfun.cn/rest/app/favorite/articleList")
    @FormUrlEncoded
    suspend fun getFavoriteArticleList(
        @Field("cursor") cursor: String,
        @Field("count") count: Int
    ): SaniouResponse<Any>

    /**
     * 收藏的合集列表
     */
    @POST("https://api-new.app.acfun.cn/rest/app/favorite/albumList")
    @FormUrlEncoded
    suspend fun getFavoriteAlbumList(
        @Field("cursor") cursor: String,
        @Field("count") count: Int
    ): SaniouResponse<Any>

    /**
     * 历史记录列表
     *
     * @param resourceTypes 1: 视频, 2: 番剧?, 3: 文章. 多个类型需重复传递参数 (Ktorfit 支持 List)
     */
    @POST("https://api-new.app.acfun.cn/rest/app/browse/history/list")
    @FormUrlEncoded
    suspend fun getHistoryList(
        @Field("pcursor") pcursor: String,
        @Field("resourceTypes") resourceTypes: List<Int>
    ): SaniouResponse<Any>

    /**
     * 删除历史记录
     */
    @POST("https://api-new.app.acfun.cn/rest/app/browse/history/delete")
    @FormUrlEncoded
    suspend fun deleteHistory(
        @Field("comboIds") comboIds: String
    ): SaniouResponse<Any>

    /**
     * 添加稍后再看
     */
    @POST("https://api-new.app.acfun.cn/rest/app/addWaiting")
    @FormUrlEncoded
    suspend fun addWaiting(
        @Field("resourceId") resourceId: Long,
        @Field("resourceType") resourceType: Int
    ): SaniouResponse<Any>

    /**
     * 稍后再看列表
     */
    @POST("https://api-new.app.acfun.cn/rest/app/waitingList")
    @FormUrlEncoded
    suspend fun getWaitingList(
        @Field("pcursor") pcursor: String,
        @Field("count") count: Int
    ): SaniouResponse<Any>

    /**
     * 取消稍后再看
     */
    @POST("https://api-new.app.acfun.cn/rest/app/cancelWaiting")
    @FormUrlEncoded
    suspend fun cancelWaiting(
        @Field("resourceIds") resourceIds: String,
        @Field("resourceType") resourceType: Int
    ): SaniouResponse<Any>
}
