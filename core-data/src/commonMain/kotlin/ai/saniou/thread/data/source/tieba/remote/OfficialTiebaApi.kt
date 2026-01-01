package ai.saniou.thread.data.source.tieba.remote

import ai.saniou.thread.data.source.tieba.model.AddPostBean
import ai.saniou.thread.data.source.tieba.model.AgreeBean
import ai.saniou.thread.data.source.tieba.model.CheckReportBean
import ai.saniou.thread.data.source.tieba.model.CommonResponse
import ai.saniou.thread.data.source.tieba.model.FollowBean
import ai.saniou.thread.data.source.tieba.model.GetForumListBean
import ai.saniou.thread.data.source.tieba.model.InitNickNameBean
import ai.saniou.thread.data.source.tieba.model.LoginBean
import ai.saniou.thread.data.source.tieba.model.MSignBean
import ai.saniou.thread.data.source.tieba.model.PersonalizedBean
import ai.saniou.thread.data.source.tieba.model.ProfileBean
import ai.saniou.thread.data.source.tieba.model.SignResultBean
import ai.saniou.thread.data.source.tieba.model.SubFloorListBean
import ai.saniou.thread.data.source.tieba.model.Sync
import ai.saniou.thread.data.source.tieba.model.ThreadContentBean
import ai.saniou.thread.data.source.tieba.model.ThreadStoreBean
import ai.saniou.thread.data.source.tieba.model.UploadPictureResultBean
import ai.saniou.thread.data.source.tieba.model.UserLikeForumBean
import ai.saniou.thread.network.tieba.TiebaApiConstants
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FieldMap
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import kotlin.time.Clock

/**
 * 移植自 com.huanchengfly.tieba.post.api.retrofit.interfaces.OfficialTiebaApi
 * 官方接口，核心功能
 */
interface OfficialTiebaApi {
    /**
     * 获取帖子内容 (按页码)
     * 对应原接口: /c/f/pb/page
     *
     * @param threadId 帖子ID
     * @param page 页码
     * @param last 倒序查看时的最后一个楼层ID
     * @param r 倒序查看时的r
     * @param lz 只看楼主 (1: 是, 0: 否)
     */
    @POST("c/f/pb/page")
    @FormUrlEncoded
    suspend fun threadContent(
        @Field("kz") threadId: String,
        @Field("pn") page: Int,
        @Field("last") last: String?,
        @Field("r") r: String?,
        @Field("lz") lz: Int,
        @Field("st_type") st_type: String = "tb_frslist",
        @Field("back") back: String = "0",
        @Field("floor_rn") floor_rn: String = "3",
        @Field("mark") mark: String = "0",
        @Field("rn") rn: String = "30",
        @Field("with_floor") with_floor: String = "1",
        @Field("scr_dip") scr_dip: String,
        @Field("scr_h") scr_h: String,
        @Field("scr_w") scr_w: String,
        @Header("thread_id") threadIdHeader: String = threadId,
    ): ThreadContentBean

    /**
     * 获取帖子内容 (按楼层ID跳转)
     * 对应原接口: /c/f/pb/page
     *
     * @param threadId 帖子ID
     * @param postId 楼层ID (跳转到指定楼层)
     */
    @POST("c/f/pb/page")
    @FormUrlEncoded
    suspend fun threadContent(
        @Field("kz") threadId: String,
        @Field("pid") postId: String?,
        @Field("last") last: String?,
        @Field("r") r: String?,
        @Field("lz") lz: Int,
        @Field("st_type") st_type: String = "tb_frslist",
        @Field("back") back: String = "0",
        @Field("floor_rn") floor_rn: String = "3",
        @Field("mark") mark: String = "0",
        @Field("rn") rn: String = "30",
        @Field("with_floor") with_floor: String = "1",
        @Field("scr_dip") scr_dip: String,
        @Field("scr_h") scr_h: String,
        @Field("scr_w") scr_w: String,
        @Header("thread_id") threadIdHeader: String = threadId,
    ): ThreadContentBean

    /**
     * 提交不感兴趣/不喜欢 (首页推荐)
     * 对应原接口: /c/c/excellent/submitDislike
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/c/excellent/submitDislike")
    @FormUrlEncoded
    suspend fun submitDislike(
        @Field("dislike") dislike: String,
        @Field("dislike_from") dislike_from: String = "homepage",
        @Field("stoken") stoken: String?
    ): CommonResponse

    /**
     * 取消关注用户
     * 对应原接口: /c/c/user/unfollow
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/c/user/unfollow")
    @FormUrlEncoded
    suspend fun unfollow(
        @Field("portrait") portrait: String,
        @Field("tbs") tbs: String,
        @Field("_client_version") client_version: String = "11.10.8.6",
        @Header("User-Agent") user_agent: String = "bdtb for Android $client_version",
        @Field("authsid") authsid: String = "null",
        @Field("stoken") stoken: String,
        @Field("from_type") fromType: Int = 2,
        @Field("in_live") inLive: Int = 0,
        @Field("timestamp") timestamp: Long = Clock.System.now().toEpochMilliseconds()
    ): CommonResponse

    /**
     * 关注用户
     * 对应原接口: /c/c/user/follow
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/c/user/follow")
    @FormUrlEncoded
    suspend fun follow(
        @Field("portrait") portrait: String,
        @Field("tbs") tbs: String,
        @Field("_client_version") client_version: String = "11.10.8.6",
        @Header("User-Agent") user_agent: String = "bdtb for Android $client_version",
        @Field("authsid") authsid: String = "null",
        @Field("stoken") stoken: String,
        @Field("from_type") fromType: Int = 2,
        @Field("in_live") inLive: Int = 0
    ): FollowBean

    /**
     * 获取用户关注的吧列表
     * 对应原接口: /c/f/forum/getforumlist
     */
    @Headers(
        "${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}",
        "No-Common-Params: BDUSS"
    )
    @POST("c/f/forum/getforumlist")
    @FormUrlEncoded
    suspend fun getForumList(
        @Field("BDUSS") bduss: String,
        @Field("stoken") stoken: String,
        @Field("user_id") userId: String,
        @Field("_client_version") client_version: String = "11.10.8.6",
        @Header("User-Agent") user_agent: String = "bdtb for Android $client_version",
    ): GetForumListBean

    /**
     * 一键签到
     * 对应原接口: /c/c/forum/msign
     *
     * @param forumIds 吧ID列表，逗号分隔
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/c/forum/msign")
    @FormUrlEncoded
    suspend fun mSign(
        @Field("forum_ids") forumIds: String,
        @Field("tbs") tbs: String,
        @Field("_client_version") client_version: String = "11.10.8.6",
        @Header("User-Agent") user_agent: String = "bdtb for Android $client_version",
        @Field("authsid") authsid: String = "null",
        @Field("stoken") stoken: String,
        @Field("user_id") userId: String
    ): MSignBean

    /**
     * 初始化昵称
     * 对应原接口: /c/s/initNickname
     */
    @Headers(
        "Cookie: ka=open",
        "Drop-Headers: Charset,client_type",
        "No-Common-Params: BDUSS"
    )
    @POST("c/s/initNickname")
    @FormUrlEncoded
    suspend fun initNickName(
        @Field("BDUSS") bduss: String,
        @Field("stoken") sToken: String,
        @Field("_client_version") client_version: String = "11.10.8.6",
        @Header("User-Agent") user_agent: String = "bdtb for Android $client_version"
    ): InitNickNameBean

    /**
     * 登录 (使用 BDUSS/SToken 换取用户信息)
     * 对应原接口: /c/s/login
     */
    @Headers(
        "Cookie: ka=open",
        "Drop-Headers: Charset,client_type",
        "Drop-Params: BDUSS"
    )
    @POST("c/s/login")
    @FormUrlEncoded
    suspend fun login(
        @Field("bdusstoken") bdusstoken: String,
        @Field("stoken") sToken: String,
        @Field("user_id") userId: String?,
        @Field("channel_id") channelId: String = "",
        @Field("channel_uid") channelUid: String = "",
        @Field("_client_version") client_version: String = "11.10.8.6",
        @Header("User-Agent") user_agent: String = "bdtb for Android $client_version",
        @Field("authsid") authsid: String = "null",
    ): LoginBean

    /**
     * 获取用户个人主页信息
     * 对应原接口: /c/u/user/profile
     */
    @Headers(
        "Cookie: ka=open",
        "Drop-Headers: Charset,client_type"
    )
    @POST("c/u/user/profile")
    @FormUrlEncoded
    suspend fun profile(
        @Field("stoken") sToken: String,
        @Field("tbs") tbs: String,
        @Field("uid") userId: String?,
        @Field("is_from_usercenter") isFromUserCenter: String = "1",
        @Field("need_post_count") needPostCount: String = "1",
        @Field("page") page: String = "1",
        @Field("pn") pn: String = "1",
        @Field("_client_version") client_version: String = "11.10.8.6",
        @Header("User-Agent") user_agent: String = "bdtb for Android $client_version",
    ): ProfileBean // Use ProfileBean from TiebaModels, assuming compatibility with WebProfile

    /**
     * 修改个人资料
     * 对应原接口: /c/c/profile/modify
     */
    @Headers(
        "Cookie: ka=open",
        "Drop-Headers: Charset,client_type"
    )
    @POST("c/c/profile/modify")
    @FormUrlEncoded
    suspend fun profileModify(
        @Field("birthday_show_status") birthdayShowStatus: String,
        @Field("birthday_time") birthdayTime: String,
        @Field("intro") intro: String,
        @Field("sex") sex: String,
        @Field("nick_name") nickName: String,
        @Field("stoken") sToken: String,
        @Field("cam") cam: String = "",
        @Field("need_cam_decrypt") needCamDecrypt: String = "1",
        @Field("need_keep_nickname_flag") needKeepNicknameFlag: String = "0",
    ): CommonResponse

    // Note: Multipart support requires a specific plugin or Ktor setup.
    // For now, defining Body as Any/Multipart type if available, or omitting detailed signature if Body type unknown.
    // Assuming Ktorfit supports @Body with multipart content.
    /**
     * 修改头像
     * 对应原接口: /c/c/img/portrait
     */
    @Headers(
        "${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}",
        "Cookie: ka=open",
        "Drop-Headers: Charset,client_type",
        "No-Common-Params: OAID"
    )
    @POST("c/c/img/portrait")
    suspend fun imgPortrait(
        @Body body: Any, // Requires platform specific multipart body
        @Header("User-Agent") user_agent: String = "bdtb for Android 11.10.8.6",
    ): CommonResponse

    /**
     * 获取首页推荐流
     * 对应原接口: /c/f/excellent/personalized
     */
    @POST("c/f/excellent/personalized")
    @FormUrlEncoded
    suspend fun personalized(
        @Field("load_type") load_type: Int,
        @Field("pn") page: Int = 1,
        @Header("client_user_token") client_user_token: String?,
        @Field("_client_version") client_version: String = "11.10.8.6",
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
     * 单个吧签到
     * 对应原接口: /c/c/forum/sign
     */
    @Headers(
        "${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}",
        "Cookie: ka=open",
        "Drop-Headers: Charset,client_type",
        "No-Common-Params: OAID",
    )
    @POST("c/c/forum/sign")
    @FormUrlEncoded
    suspend fun sign(
        @Field("fid") forumId: String,
        @Field("kw") forumName: String,
        @Field("tbs") tbs: String,
        @Header("client_user_token") client_user_token: String?,
        @Field("_client_version") client_version: String = "11.10.8.6",
        @Header("User-Agent") user_agent: String = "bdtb for Android $client_version",
    ): SignResultBean

    /**
     * 取消关注吧 (API名疑似拼写错误，原项目如此)
     * 对应原接口: /c/c/forum/unfavolike
     */
    @POST("c/c/forum/unfavolike")
    @FormUrlEncoded
    @Headers(
        "${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}",
        "Cookie: ka=open",
        "Drop-Headers: Charset,client_type",
        "No-Common-Params: OAID",
    )
    suspend fun unfavolike(
        @Field("fid") forumId: String,
        @Field("kw") forumName: String,
        @Field("tbs") tbs: String,
        @Header("client_user_token") client_user_token: String?,
        @Field("_client_version") client_version: String = "11.10.8.6",
        @Header("User-Agent") user_agent: String = "bdtb for Android $client_version",
        @Field("stoken") stoken: String?,
    ): CommonResponse

    /**
     * 获取收藏帖子列表
     * 对应原接口: /c/f/post/threadstore
     */
    @POST("c/f/post/threadstore")
    @FormUrlEncoded
    @Headers(
        "${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}",
        "Cookie: ka=open",
        "Drop-Headers: Charset,client_type",
        "No-Common-Params: OAID",
    )
    suspend fun threadStore(
        @Field("rn") pageSize: Int,
        @Field("offset") offset: Int,
        @Header("client_user_token") client_user_token: String?,
        @Field("_client_version") client_version: String = "11.10.8.6",
        @Header("User-Agent") user_agent: String = "bdtb for Android $client_version",
        @Field("stoken") stoken: String?,
        @Field("user_id") user_id: String?,
    ): ThreadStoreBean

    /**
     * 启动/同步配置
     * 对应原接口: /c/s/sync
     */
    @Headers(
        "Drop-Headers: Charset,client_type",
        "No-Common-Params: OAID,MAC,PHONE_IMEI,ANDROID_ID,SWAN_GAME_VER,SDK_VER",
    )
    @POST("c/s/sync")
    @FormUrlEncoded
    suspend fun sync(
        @Field("client_id") clientId: String?,
        @Field("_msg_status") msgStatus: String = "1",
        @Field("_phone_screen") phoneScreen: String,
        @Field("_pic_quality") picQuality: String = "0",
        @Field("board") board: String,
        @Field("brand") brand: String,
        @Field("cam") cam: String,
        @Field("di_diordna") androidIdR: String,
        @Field("iemi") imeiR: String,
        @Field("incremental") incremental: String,
        @Field("md5") md5: String = "F86F4C238491AB3BEBFA33AC42C1582B",
        @Field("signmd5") signmd5: String = "225172691",
        @Field("package") packageName: String = "com.baidu.tieba",
        @Field("versioncode") versionCode: String = "202965248",
        @Field("running_abi") runningAbi: Int = 64,
        @Field("support_abi") supportAbi: Int = 64,
        @Field("scr_dip") scr_dip: String,
        @Field("scr_h") scr_h: String,
        @Field("scr_w") scr_w: String,
        @Field("stoken") sToken: String?,
        @Header("Cookie") cookie: String
    ): Sync

    /**
     * 发布帖子/回复
     * 对应原接口: /c/c/post/add
     */
    @Headers(
        "${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}",
        "Cookie: ka=open",
        "Drop-Headers: Charset,client_type",
        "No-Common-Params: OAID",
    )
    @POST("c/c/post/add")
    @FormUrlEncoded
    suspend fun addPost(
        @Field("content") content: String,
        @Field("fid") forumId: String,
        @Field("kw") forumName: String,
        @Field("tbs") tbs: String,
        @Field("tid") threadId: String,
        @Field("quote_id") quoteId: String? = null,
        @Field("repostid") repostId: String? = null,
        @Field("reply_uid") replyUserId: String = "null",
        @Field("name_show") nameShow: String?,
        @Field("anonymous") anonymous: String = "1",
        @Field("authsid") authsid: String = "null",
        @Field("barrage_time") barrage_time: String = "0",
        @Field("can_no_forum") can_no_forum: String = "0",
        @Field("entrance_type") entrance_type: String = "0",
        @Field("from_fourm_id") from_fourm_id: String = "null",
        @Field("is_ad") is_ad: String = "0",
        @Field("is_addition") is_addition: String? = null,
        @Field("is_barrage") is_barrage: String? = "0",
        @Field("is_feedback") is_feedback: String = "0",
        @Field("is_giftpost") is_giftpost: String? = null,
        @Field("is_twzhibo_thread") is_twzhibo_thread: String? = null,
        @Field("new_vcode") new_vcode: String = "1",
        @Field("post_from") post_from: String = "3",
        @Field("takephoto_num") takephoto_num: String = "0",
        @Field("v_fid") v_fid: String = "",
        @Field("v_fname") v_fname: String = "",
        @Field("vcode_tag") vcode_tag: String = "12",
        @Field("_client_version") client_version: String = "11.10.8.6",
        @Header("User-Agent") user_agent: String = "bdtb for Android $client_version",
        @Field("stoken") sToken: String?,
        @Header("client_user_token") client_user_token: String?,
    ): AddPostBean

    /**
     * 取消收藏帖子
     * 对应原接口: /c/c/post/rmstore
     */
    @POST("c/c/post/rmstore")
    @FormUrlEncoded
    @Headers(
        "${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}",
        "Cookie: ka=open",
        "Drop-Headers: Charset,client_type",
        "No-Common-Params: OAID",
    )
    suspend fun removeStore(
        @Field("tid") threadId: String,
        @Field("fid") forumId: String = "null",
        @Field("tbs") tbs: String,
        @Field("stoken") stoken: String,
        @Field("user_id") user_id: String?,
        @Header("client_user_token") client_user_token: String?,
    ): CommonResponse

    /**
     * 添加收藏帖子
     * 对应原接口: /c/c/post/addstore
     */
    @POST("c/c/post/addstore")
    @FormUrlEncoded
    @Headers(
        "${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}",
        "Cookie: ka=open",
        "Drop-Headers: Charset,client_type",
        "No-Common-Params: OAID",
    )
    suspend fun addStore(
        @Field("data") data: String,
        @Field("stoken") stoken: String = "",
        @Header("client_user_token") client_user_token: String?,
    ): CommonResponse

    /**
     * 点赞/取消点赞
     * 对应原接口: /c/c/agree/opAgree
     */
    @Headers(
        "${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}",
        "Cookie: ka=open",
        "Drop-Headers: Charset,client_type",
        "No-Common-Params: OAID",
    )
    @POST("c/c/agree/opAgree")
    @FormUrlEncoded
    suspend fun agree(
        @Field("thread_id") threadId: String,
        @Field("post_id") postId: String? = null,
        @Field("op_type") opType: Int = 0,
        @Field("obj_type") objType: Int = 1,
        @Field("agree_type") agreeType: Int = 2,
        @Header("client_user_token") client_user_token: String?,
        @Field("cuid_gid") cuid_gid: String = "",
        @Field("forum_id") forumId: String = "",
        @Field("personalized_rec_switch") personalizedRecSwitch: Int = 1,
        @Field("tbs") tbs: String,
        @Field("stoken") stoken: String
    ): AgreeBean

    /**
     * 上传图片
     * 对应原接口: /c/s/uploadPicture
     */
    @Headers(
        "${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}",
        "Drop-Headers: Charset,client_type",
        "No-Common-Params: SWAN_GAME_VER,SDK_VER",
    )
    @POST("c/s/uploadPicture")
    suspend fun uploadPicture(
        @Body body: Any, // Requires multipart
        @Header("Cookie") cookie: String,
    ): UploadPictureResultBean

    /**
     * 举报检查
     * 对应原接口: /c/f/ueg/checkjubao
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/f/ueg/checkjubao")
    @FormUrlEncoded
    suspend fun checkReport(
        @Field("category") category: String,
        @FieldMap reportParam: Map<String, String>,
        @Field("stoken") stoken: String?,
    ): CheckReportBean

    /**
     * 吧务删除帖子
     * 对应原接口: /c/c/bawu/delthread
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/c/bawu/delthread")
    @FormUrlEncoded
    suspend fun delThread(
        @Field("fid") forumId: Long,
        @Field("word") forumName: String,
        @Field("z") threadId: Long,
        @Field("tbs") tbs: String?,
        @Field("src") src: Int = 1,
        @Field("is_vipdel") isVipDel: Int = 0,
        @Field("delete_my_thread") deleteMyThread: Int = 1,
        @Field("is_frs_mask") isFrsMask: Int = 0,
    ): CommonResponse

    /**
     * 吧务删除楼层/回复
     * 对应原接口: /c/c/bawu/delpost
     */
    @Headers("${TiebaApiConstants.FORCE_PARAM}: ${TiebaApiConstants.FORCE_PARAM_QUERY}")
    @POST("c/c/bawu/delpost")
    @FormUrlEncoded
    suspend fun delPost(
        @Field("fid") forumId: Long,
        @Field("word") forumName: String,
        @Field("z") threadId: Long,
        @Field("pid") postId: Long,
        @Field("isfloor") isFloor: Int,
        @Field("src") src: Int,
        @Field("is_vipdel") isVipDel: Int,
        @Field("delete_my_post") deleteMyPost: Int,
        @Field("tbs") tbs: String?,
    ): CommonResponse

    /**
     * 获取用户关注的吧列表 (Forum Like)
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
        @Field("is_guest") is_guest: String?,
    ): UserLikeForumBean

    /**
     * 获取楼中楼列表
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
}