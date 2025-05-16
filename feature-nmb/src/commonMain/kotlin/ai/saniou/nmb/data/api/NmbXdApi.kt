package ai.saniou.nmb.data.api;

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.CdnPath
import ai.saniou.nmb.data.entity.CookieListResponse
import ai.saniou.nmb.data.entity.Feed
import ai.saniou.nmb.data.entity.ForumCategory
import ai.saniou.nmb.data.entity.LastPost
import ai.saniou.nmb.data.entity.LoginRequest
import ai.saniou.nmb.data.entity.LoginResponse
import ai.saniou.nmb.data.entity.NmbNotice
import ai.saniou.nmb.data.entity.NmbReference
import ai.saniou.nmb.data.entity.PostReplyRequest
import ai.saniou.nmb.data.entity.PostThreadRequest
import ai.saniou.nmb.data.entity.ShowF
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.TimeLine
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Query

interface NmbXdApi {
    /**
     * 获取图片 CDN 地址
     */
    @GET("getCDNPath")
    suspend fun getCdnPath(): SaniouResponse<List<CdnPath>>

    /**
     * 获取备用 API 链接的接口的路径
     */
    @GET("backupUrl")
    suspend fun backupUrl(): String

    /**
     * 版面列表
     *
     * 这个 API 所有本来应该返回 Number 的字段都被弄成了 String，需要注意类型转换问题。
     * 请忽略那个版面 ID 为 -1 的“时间线”板块。获取时间线相关的信息有其他的 API。
     * [].forums[].showName 和 [].forums[].name 的区别在于侧边的导航栏会优先使用前者作为版面名称，只有前者为空的情况下才会使用后者。例如：技术版在导航栏显示为“技术(码农)”，但进入版面后显示的名称为“技术宅”。
     *
     */
    @GET("getForumList")
    suspend fun getForumList(): SaniouResponse<List<ForumCategory>>

    /**
     * 时间线列表
     */
    @GET("getTimelineList")
    suspend fun getTimelineList(): SaniouResponse<List<TimeLine>>

    /**
     * 查看版面
     *
     *
     * 实际的图片地址由 CDN 地址和 img、ext 两个字段组合而成。例如：图片 CDN 地址为 https://image.nmb.best/，img 为 2022-06-18/62acedc59ef24，ext 为 .png，则图片地址为 https://image.nmb.best/image/2022-06-18/62acedc59ef24.png，缩略图地址为 https://image.nmb.best/thumb/2022-06-18/62acedc59ef24.png。
     * 部分版面需要饼干才能查看。
     *
     */
    @GET("showf")
    suspend fun showf(
        @Query("id") id: Long,//版面 ID
        @Query("page") page: Long,//页数，默认为 1
    ): SaniouResponse<List<ShowF>>

    /**
     * 查看时间线
     *
     * 和“查看版面”相同，不再重复。
     * 部分时间线需要饼干才能查看。
     *
     */
    @GET("timeline")
    suspend fun timeline(
        @Query("id") id: Long,//版面 ID
        @Query("page") page: Long,//页数，默认为 1
    ): SaniouResponse<List<ShowF>>

    /**
     * 查看串
     *
     * 回复数据中可能会出现 Tips 酱，具体特征可以参见上面的示例。
     */
    @GET("thread")
    suspend fun thread(
        @Query("id") id: Long,//串 ID
        @Query("page") page: Long,//页数，默认为 1
    ): SaniouResponse<Thread>

    /**
     * 查看串（只看 PO）
     *
     * 回复数据中可能会出现 Tips 酱，具体特征可以参见上面的示例。
     */
    @GET("po")
    suspend fun po(
        @Query("id") id: Long,//串 ID
        @Query("page") page: Long,//页数，默认为 1
    ): SaniouResponse<Thread>

    /**
     * 查看引用
     *
     * 可以查看串和回复的内容，但是并没有办法获取回复所属的串号。
     */
    @GET("ref")
    suspend fun ref(
        @Query("id") id: Long,//串 ID
    ): SaniouResponse<NmbReference>

    /**
     * 发串
     *
     *
     * 发串和回复串没有 JSON API，只能使用网页版的 API，域名为匿名版主站的域名。
     * 串的内容和附加图片不能同时为空。
     * water 的值遵从 PHP 的类型转换到 bool 的规则。例如：不写 water 字段或留空或设为 0 均不会添加水印，设为 true 或 foobar 等值则会添加水印。
     *
     * 返回值为 HTML 格式的页面，可以直接复制到浏览器中打开。
     */
    @POST("https://www.nmbxd.com/home/forum/doPostThread.html")
//    @FormUrlEncoded
    suspend fun postThread(
        @Body body: PostThreadRequest
    ): String

    /**
     * 回复串
     */
    @POST("https://www.nmbxd.com/home/forum/doReplyThread.html")
//    @FormUrlEncoded
    suspend fun postReply(
        @Body body: PostReplyRequest
    ): String

    /**
     * 查看订阅
     *
     * 订阅 ID 的字段虽然名为 uuid，但是实际上并不需要遵守 UUID 的格式。包括空字符串在内的任意长度的字符串都可以作为订阅 ID 使用。
     * 这个 API 同样把所有本来应该返回 Number 的字段弄成了 String，需要注意类型转换问题
     */
    @GET("feed")
    suspend fun feed(
        @Query("uuid") uuid: String,//订阅 UUID
        @Query("page") page: Long,//页数，默认为 1
    ): SaniouResponse<List<Feed>>

    /**
     * 添加订阅
     *
     * 即使已经使用这个订阅 ID 订阅过某个串，再次订阅时仍然会提示订阅成功。并没有办法获取某个串是否已经订阅过。
     *
     * response:"订阅大成功→_→" / "该串不存在"
     */
    @POST("addFeed")
    @FormUrlEncoded
    suspend fun addFeed(
        @Query uuid: String,//订阅 UUID
        @Field tid: Long,// 串的 ID
    ): String

    /**
     * 取消订阅
     *
     * 即使并没有使用这个订阅 ID 订阅过某个串或串本身不存在，取消订阅时仍然会提示取消订阅成功。
     *
     * response:"取消订阅成功!"
     */
    @POST("delFeed")
    @FormUrlEncoded
    suspend fun delFeed(
        @Query uuid: String,//订阅 UUID
        @Field tid: Long,// 串的 ID
    ): String

    /**
     * 获取最新发的串的链接
     * 只能在发串/回复后的大约 3 秒内从这个 API 查到数据，否则会返回 []。
     */
    @GET("getLastPost")
    suspend fun getLastPost(
        @Query("id") id: Long,//串 ID
        @Query("page") page: Long,//页数，默认为 1
    ): SaniouResponse<LastPost>

    /**
     * 查看匿名版公告
     */

    @GET("https://nmb.ovear.info/nmb-notice.json")
    suspend fun notice(): SaniouResponse<NmbNotice>

    /**
     * 随机封面图
     *
     * 占位，直接加载即可
     */
    @GET("https://nmb.ovear.info/h.php")
    suspend fun greetImage(): String

    /**
     * 获取验证码图片
     */
    @GET("https://www.nmbxd.com/Member/User/Index/verify.html")
    suspend fun getVerifyImage(): String

    /**
     * 用户登录
     */
    @POST("https://www.nmbxd.com/Member/User/Index/login.html")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    /**
     * 获取用户饼干列表
     */
    @GET("https://www.nmbxd.com/Member/User/Cookie/index.html")
    suspend fun getCookiesList(): CookieListResponse

    /**
     * 申请新饼干
     */
    @POST("https://www.nmbxd.com/Member/User/Cookie/apply.html")
    suspend fun applyNewCookie(): String

    /**
     * 注册账号
     */
    @POST("https://www.nmbxd.com/Member/User/Index/sendRegister.html")
    @FormUrlEncoded
    suspend fun register(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("password_confirm") passwordConfirm: String,
        @Field("verify") verify: String
    ): String

    /**
     * 重置密码
     */
    @POST("https://www.nmbxd.com/Member/User/Index/sendForgotPassword.html")
    @FormUrlEncoded
    suspend fun resetPassword(
        @Field("email") email: String,
        @Field("verify") verify: String
    ): String
}
