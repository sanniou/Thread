package ai.saniou.thread.data.source.tieba.remote

import ai.saniou.thread.data.source.tieba.model.AgreeBean
import ai.saniou.thread.data.source.tieba.model.CheckReportBean
import ai.saniou.thread.data.source.tieba.model.CommonResponse
import ai.saniou.thread.data.source.tieba.model.ForumPageBean
import ai.saniou.thread.data.source.tieba.model.ForumRecommend
import ai.saniou.thread.data.source.tieba.model.LikeForumResultBean
import ai.saniou.thread.data.source.tieba.model.PersonalizedBean
import ai.saniou.thread.data.source.tieba.model.PicPageBean
import ai.saniou.thread.data.source.tieba.model.ProfileBean
import ai.saniou.thread.data.source.tieba.model.SearchPostBean
import ai.saniou.thread.data.source.tieba.model.SearchUserBean
import ai.saniou.thread.data.source.tieba.model.SignResultBean
import ai.saniou.thread.data.source.tieba.model.SubFloorListBean
import ai.saniou.thread.data.source.tieba.model.UserLikeForumBean
import ai.saniou.thread.data.source.tieba.model.UserPostBean
import ai.saniou.thread.network.tieba.TiebaApiConstants
import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FieldMap
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Query
import kotlin.time.Clock

/**
 * 移植自 com.huanchengfly.tieba.post.api.retrofit.interfaces.MiniTiebaApi
 */
interface MiniTiebaApi {
    /**
     * 获取个性化推荐内容 (Mini 版)
     *
     * @param load_type 加载类型
     * @param page 页码
     * @param client_user_token 客户端用户 Token (uid)
     * @param client_version 客户端版本
     * @param user_agent User-Agent
     * @param cuid_gid CUID GID
     * @param need_tags 是否需要标签
     * @param page_thread_count 每页帖子数
     * @param pre_ad_thread_count 预加载广告帖子数
     * @param sug_count 建议数量
     * @param tag_code 标签代码
     * @param q_type 查询类型
     * @param need_forumlist 是否需要板块列表
     * @param new_net_type 网络类型
     * @param new_install 是否新安装
     * @param request_time 请求时间戳
     * @param invoke_source 调用来源
     * @param scr_dip 屏幕密度
     * @param scr_h 屏幕高度
     * @param scr_w 屏幕宽度
     * @return 个性化推荐结果
     */
    @POST("c/f/excellent/personalized")
    @FormUrlEncoded
    suspend fun personalized(
        @Field("load_type") load_type: Int,
        @Field("pn") page: Int = 1,
        @Header("client_user_token") client_user_token: String?,
        @Field("_client_version") client_version: String = "8.0.8.0",
        @Header("User-Agent") user_agent: String = "bdtb for Android $client_version",
        @Field("cuid_gid") cuid_gid: String = "",
        @Field("need_tags") need_tags: Int = 0,
        @Field("page_thread_count") page_thread_count: Int = 15,
        @Field("pre_ad_thread_count") pre_ad_thread_count: Int = 0,
        @Field("sug_count") sug_count: Int = 0,
        @Field("tag_code") tag_code: Int = 0,
        @Field("q_type") q_type: Int = 1,
        @Field("need_forumlist") need_forumlist: Int = 0,
        @Field("new_net_type") new_net_type: Int = 1,
        @Field("new_install") new_install: Int = 0,
        @Field("request_time") request_time: Long = Clock.System.now().toEpochMilliseconds(),
        @Field("invoke_source") invoke_source: String = "",
        @Field("scr_dip") scr_dip: String,
        @Field("scr_h") scr_h: String,
        @Field("scr_w") scr_w: String
    ): PersonalizedBean

    /**
     * 点赞操作
     *
     * @param postId 帖子/回复 ID
     * @param threadId 主题 ID
     * @param client_user_token 客户端用户 Token
     * @param client_version 客户端版本
     * @param user_agent User-Agent
     * @param cuid_gid CUID GID
     * @param agree_type 点赞类型 (2: 点赞)
     * @param obj_type 对象类型 (3: 帖子/回复)
     * @param op_type 操作类型 (0: 点赞)
     * @param tbs TBS Token
     * @param stoken SToken
     * @return 点赞结果
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/c/agree/opAgree")
    @FormUrlEncoded
    suspend fun agree(
        @Field("post_id") postId: String,
        @Field("thread_id") threadId: String,
        @Header("client_user_token") client_user_token: String?,
        @Field("_client_version") client_version: String = "8.0.8.0",
        @Header("User-Agent") user_agent: String = "bdtb for Android $client_version",
        @Field("cuid_gid") cuid_gid: String = "",
        @Field("agree_type") agree_type: Int = 2,
        @Field("obj_type") obj_type: Int = 3,
        @Field("op_type") op_type: Int = 0,
        @Field("tbs") tbs: String?,
        @Field("stoken") stoken: String?
    ): AgreeBean

    /**
     * 取消点赞操作
     *
     * @param postId 帖子/回复 ID
     * @param threadId 主题 ID
     * @param client_user_token 客户端用户 Token
     * @param client_version 客户端版本
     * @param user_agent User-Agent
     * @param cuid_gid CUID GID
     * @param agree_type 点赞类型 (2: 点赞)
     * @param obj_type 对象类型 (3: 帖子/回复)
     * @param op_type 操作类型 (1: 取消点赞)
     * @param tbs TBS Token
     * @param stoken SToken
     * @return 取消点赞结果
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/c/agree/opAgree")
    @FormUrlEncoded
    suspend fun disagree(
        @Field("post_id") postId: String,
        @Field("thread_id") threadId: String,
        @Header("client_user_token") client_user_token: String?,
        @Field("_client_version") client_version: String = "8.0.8.0",
        @Header("User-Agent") user_agent: String = "bdtb for Android $client_version",
        @Field("cuid_gid") cuid_gid: String = "",
        @Field("agree_type") agree_type: Int = 2,
        @Field("obj_type") obj_type: Int = 3,
        @Field("op_type") op_type: Int = 1,
        @Field("tbs") tbs: String,
        @Field("stoken") stoken: String
    ): AgreeBean

    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/f/forum/forumrecommend")
    @FormUrlEncoded
    suspend fun forumRecommend(
        @Field("like_forum") like_forum: String = "1",
        @Field("recommend") recommend: String = "0",
        @Field("topic") topic: String = "0"
    ): ForumRecommend

    /**
     * 获取吧帖子列表 (Mini 版)
     * 对应原接口: /c/f/frs/page
     */
    @POST("c/f/frs/page")
    @FormUrlEncoded
    suspend fun forumPage(
        @Field("kw") forumName: String,
        @Field("pn") page: Int = 1,
        @Field("sort_type") sort_type: Int,
        @Field("cid") goodClassifyId: String? = null,
        @Field("is_good") is_good: String? = null,
        @Field("q_type") q_type: String = "2",
        @Field("st_type") st_type: String = "tb_forumlist",
        @Field("with_group") with_group: String = "0",
        @Field("rn") rn: String = "20",
        @Field("scr_dip") scr_dip: String,
        @Field("scr_h") scr_h: String,
        @Field("scr_w") scr_w: String
    ): ForumPageBean

    /**
     * 获取楼中楼列表 (Mini 版)
     * 对应原接口: /c/f/pb/floor
     */
    @POST("c/f/pb/floor")
    @FormUrlEncoded
    suspend fun floor(
        @Field("kz") threadId: String,
        @Field("pn") page: Int = 1,
        @Field("pid") postId: String?,
        @Field("spid") subPostId: String?,
        @Field("rn") rn: Int = 20
    ): SubFloorListBean

    /**
     * 获取用户关注的吧列表 (Mini 版)
     * 对应原接口: /c/f/forum/like
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/f/forum/like")
    @FormUrlEncoded
    suspend fun userLikeForum(
        @Field("page_no") page: Int = 1,
        @Field("page_size") pageSize: Int = 50,
        @Field("uid") uid: String?,
        @Field("friend_uid") friendUid: String?,
        @Field("is_guest") is_guest: String?
    ): UserLikeForumBean

    /**
     * 获取用户发布列表 (Mini 版)
     * 对应原接口: /c/u/feed/userpost
     */
    @POST("c/u/feed/userpost")
    @FormUrlEncoded
    suspend fun userPost(
        @Field("uid") uid: String,
        @Field("pn") page: Int = 1,
        @Field("is_thread") is_thread: Int,
        @Field("rn") pageSize: Int = 20,
        @Field("need_content") need_content: Int = 1
    ): UserPostBean

    /**
     * 获取图片详情页 (Mini 版)
     * 对应原接口: /c/f/pb/picpage
     */
    @POST("c/f/pb/picpage")
    @FormUrlEncoded
    suspend fun picPage(
        @Field("forum_id") forumId: String,
        @Field("kw") forumName: String,
        @Field("tid") threadId: String,
        @Field("pic_id") picId: String,
        @Field("pic_index") picIndex: String,
        @Field("obj_type") objType: String,
        @Field("page_name") page_name: String = "PB",
        @Field("next") next: Int = 10,
        @Field("user_id") myUid: String?,
        @Field("scr_h") scr_h: String,
        @Field("scr_w") scr_w: String,
        @Field("q_type") q_type: Int = 2,
        @Field("prev") prev: Int,
        @Field("not_see_lz") not_see_lz: Int
    ): PicPageBean

    /**
     * 获取用户个人主页信息 (Mini 版)
     * 对应原接口: /c/u/user/profile
     */
    @POST("c/u/user/profile")
    @FormUrlEncoded
    suspend fun profile(
        @Field("uid") uid: String,
        @Field("need_post_count") need_post_count: Int = 1
    ): ProfileBean

    /**
     * 取消关注吧 (Mini 版)
     * 对应原接口: /c/c/forum/unlike
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/c/forum/unlike")
    @FormUrlEncoded
    suspend fun unlikeForum(
        @Field("fid") forumId: String,
        @Field("kw") forumName: String,
        @Field("tbs") tbs: String?
    ): CommonResponse

    /**
     * 关注吧 (Mini 版)
     * 对应原接口: /c/c/forum/like
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/c/forum/like")
    @FormUrlEncoded
    suspend fun likeForum(
        @Field("fid") forumId: String,
        @Field("kw") forumName: String,
        @Field("tbs") tbs: String?
    ): LikeForumResultBean

    /**
     * 签到 (Mini 版)
     * 对应原接口: /c/c/forum/sign
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/c/forum/sign")
    @FormUrlEncoded
    suspend fun sign(
        @Field("kw") forumName: String,
        @Field("tbs") tbs: String
    ): SignResultBean

    /**
     * 吧务删除帖子 (Mini 版)
     * 对应原接口: /c/c/bawu/delthread
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/c/bawu/delthread")
    @FormUrlEncoded
    suspend fun delThread(
        @Field("fid") forumId: String,
        @Field("word") forumName: String,
        @Field("z") threadId: String,
        @Field("tbs") tbs: String,
        @Field("src") src: Int = 1,
        @Field("is_vipdel") is_vip_del: Int = 0,
        @Field("delete_my_post") delete_my_post: Int = 1
    ): CommonResponse

    /**
     * 吧务删除楼层 (Mini 版)
     * 对应原接口: /c/c/bawu/delpost
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/c/bawu/delpost")
    @FormUrlEncoded
    suspend fun delPost(
        @Field("fid") forumId: String,
        @Field("word") forumName: String,
        @Field("z") threadId: String,
        @Field("pid") postId: String,
        @Field("tbs") tbs: String,
        @Field("isfloor") is_floor: Int,
        @Field("src") src: Int,
        @Field("is_vipdel") is_vip_del: Int,
        @Field("delete_my_post") delete_my_post: Int
    ): CommonResponse

    /**
     * 搜索帖子 (Mini 版)
     * 对应原接口: /c/s/searchpost
     */
    @POST("c/s/searchpost")
    @FormUrlEncoded
    suspend fun searchPost(
        @Field("word") keyword: String,
        @Field("kw") forumName: String,
        @Field("pn") page: Int = 1,
        @Field("rn") pageSize: Int = 30,
        @Field("only_thread") only_thread: Int = 0,
        @Field("sm") sortMode: Int = 1
    ): SearchPostBean

    /**
     * 搜索用户 (Mini 版)
     * 对应原接口: /mo/q/search/user
     */
    @GET("mo/q/search/user")
    suspend fun searchUser(
        @Query("word") keyword: String,
        @Header("client_user_token") client_user_token: String?,
        @Query("_client_version") client_version: String = "8.0.8.0",
        @Header("User-Agent") user_agent: String = "bdtb for Android $client_version",
        @Query("cuid_gid") cuid_gid: String = ""
    ): SearchUserBean

    /**
     * 举报检查 (Mini 版)
     * 对应原接口: /c/f/ueg/checkjubao
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/f/ueg/checkjubao")
    @FormUrlEncoded
    suspend fun checkReport(
        @Field("category") category: String,
        @FieldMap reportParam: Map<String, String>,
        @Field("stoken") stoken: String?
    ): CheckReportBean
}
