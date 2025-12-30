package ai.saniou.thread.data.source.nmb.remote;

import ai.saniou.thread.network.SaniouResult
import ai.saniou.thread.data.source.nmb.remote.dto.CdnPath
import ai.saniou.thread.data.source.nmb.remote.dto.CookieListResponse
import ai.saniou.thread.data.source.nmb.remote.dto.Feed
import ai.saniou.thread.data.source.nmb.remote.dto.Forum
import ai.saniou.thread.data.source.nmb.remote.dto.ForumCategory
import ai.saniou.thread.data.source.nmb.remote.dto.LastPost
import ai.saniou.thread.data.source.nmb.remote.dto.LoginRequest
import ai.saniou.thread.data.source.nmb.remote.dto.LoginResponse
import ai.saniou.thread.data.source.nmb.remote.dto.NmbNotice
import ai.saniou.thread.data.source.nmb.remote.dto.NmbReference
import ai.saniou.thread.data.source.nmb.remote.dto.Thread
import ai.saniou.thread.data.source.nmb.remote.dto.TimeLine
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Multipart
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Part
import de.jensklingenberg.ktorfit.http.Query
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI

interface NmbXdApi {
    /**
     * 获取图片 CDN 地址
     */
    @GET("getCDNPath")
    suspend fun getCdnPath(): SaniouResult<List<CdnPath>>

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
    suspend fun getForumList(): SaniouResult<List<ForumCategory>>

    /**
     * 时间线列表
     */
    @GET("getTimelineList")
    suspend fun getTimelineList(): SaniouResult<List<TimeLine>>

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
    ): SaniouResult<List<Forum>>

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
    ): SaniouResult<List<Forum>>

    /**
     * 查看串
     *
     * 回复数据中可能会出现 Tips 酱，具体特征可以参见上面的示例。
     */
    @GET("thread")
    suspend fun thread(
        @Query("id") id: Long,//串 ID
        @Query("page") page: Long,//页数，默认为 1
    ): SaniouResult<Thread>

    /**
     * 查看串（只看 PO）
     *
     * 回复数据中可能会出现 Tips 酱，具体特征可以参见上面的示例。
     */
    @GET("po")
    suspend fun po(
        @Query("id") id: Long,//串 ID
        @Query("page") page: Long,//页数，默认为 1
    ): SaniouResult<Thread>

    /**
     * 获取最新趋势 (Trend)
     *
     * 该接口实际上是获取特定的“趋势串” (ID: 50248044) 的最新回复。
     * 客户端逻辑需自行处理分页（先获取第一页得知总页数，再获取最后一页的最后一条回复）。
     */
    @GET("thread")
    suspend fun getTrendThread(
        @Query("id") id: Long = 50248044,
        @Query("page") page: Long
    ): SaniouResult<Thread>

    /**
     * 查看引用
     *
     * 可以查看串和回复的内容，但是并没有办法获取回复所属的串号。
     */
    @GET("ref")
    suspend fun ref(
        @Query("id") id: Long,//串 ID
    ): SaniouResult<NmbReference>

    /**
     * 查看引用 (HTML)
     *
     * api 无法提供 thread 信息，html 中可以获取
     * href 中包含了 thread id
     * <a href="/t/67475759?r=67475759&scrollInto=true" class="h-threads-info-id">No.67475759</a>
     * <a href="?r=52946898&page=1&scrollInto=true" class="h-threads-info-id">No.52946898</a>
     *
     *
     * <div class="h-threads-item">
     *     <div data-threads-id="52946898" class="h-threads-item-reply h-threads-item-ref">
     *     <div class="h-threads-item-reply-main">
     *          <div class="h-threads-info">
     *         <span class="h-threads-info-title">无标题</span>
     *         <span class="h-threads-info-email" color="red">无名氏</span>
     *                 <span class="h-threads-info-createdat">2022-10-25(二)10:09:25</span>
     *         <span class="h-threads-info-uid">ID:<font color="red">Admin</font></span>
     *                 <span class="h-threads-info-report-btn">[<a href="/f/值班室?r=52946898">举报</a>]</span>
     *                 <a href="?r=52946898&page=1&scrollInto=true" class="h-threads-info-id">No.52946898</a>        <span data-uk-dropdown="data-uk-dropdown" class="h-admin-tool uk-button-dropdown"><a href="#" class="uk-button uk-button-link uk-button-small">管理</a>
     *                  <div class="uk-dropdown uk-dropdown-small">
     *                     <ul class="uk-nav uk-nav-dropdown">
     *                     <li class="uk-nav-header">管理 -&gt; No.52946898</li>
     *                     <li><a href="/Home/Forum/sagePost/id/52946898.html">SAGE</a></li>
     *                     <li><a href="http://hacfun.tv:1336/content/threads/6187238/set?key=lock&amp;value=true">锁定</a></li><li class="uk-nav-divider"></li>
     *                     <li><a href="http://hacfun.tv:1336/content/threads/6187238/removeImages">删图</a></li>
     *                     <li><a href="/Home/Forum/delPost/id/52946898.html">删串</a></li>
     *                     <li class="uk-nav-divider"></li>
     *                     <li><a href="http://hacfun.tv:1336/content/threads/6187238/update">编辑</a></li>
     *                     <li><a href="http://hacfun.tv:1336/content/threads?parent=6187238">查询</a></li>
     *                     <li><a href="/Admin/Member/queryUser/cookie/Admin.html">Cookie查询</a></li>
     *                     <li><a href="/Home/Forum/banip/id/52946898.html" class="qlink">锁IP</a>
     *                     <li><a href="/Home/Forum/banCookie/id/52946898.html" class="qlink">锁Cookie</a>
     *                     </ul>
     *                  </div></span>
     *      </div>
     *     <div class="h-threads-content">
     *         占位    </div>
     *             <div class="uk-clearfix"></div>
     *     </div>
     *     </div>
     * </div>
     *
     */
    @GET("https://www.nmbxd1.com/Home/Forum/ref")
    suspend fun refHtml(
        @Query("id") id: Long
    ): String

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
    @POST("https://www.nmbxd1.com/home/forum/doPostThread.html")
    @Multipart
    suspend fun postThread(
        @Part("fid") fid: Int,
        @Part("content") content: String,
        @Part("") parts: List<PartData> = emptyList()
    ): String

    /**
     * 回复串
     */
    @POST("https://www.nmbxd1.com/home/forum/doReplyThread.html")
    @Multipart
    suspend fun postReply(
        @Part("resto") resto: Int,
        @Part("content") content: String,
        @Part("") parts: List<PartData> = emptyList()
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
    ): SaniouResult<List<Feed>>

    /**
     * 添加订阅
     *
     * 即使已经使用这个订阅 ID 订阅过某个串，再次订阅时仍然会提示订阅成功。并没有办法获取某个串是否已经订阅过。
     *
     * response:"订阅大成功→_→" / "该串不存在"
     */
    @POST("addFeed")
    suspend fun addFeed(
        @Query("uuid") uuid: String,//订阅 UUID
        @Query("tid") tid: Long,// 串的 ID
    ): String

    /**
     * 取消订阅
     *
     * 即使并没有使用这个订阅 ID 订阅过某个串或串本身不存在，取消订阅时仍然会提示取消订阅成功。
     *
     * response:"取消订阅成功!"
     */
    @POST("delFeed")
    suspend fun delFeed(
        @Query("uuid") uuid: String,//订阅 UUID
        @Query("tid") tid: Long,// 串的 ID
    ): String

    /**
     * 获取最新发的串的链接
     * 只能在发串/回复后的大约 3 秒内从这个 API 查到数据，否则会返回 []。
     */
    @GET("https://www.nmbxd1.com/Api/getLastPost")
    suspend fun getLastPost(): SaniouResult<LastPost>

    /**
     * 查看匿名版公告
     */

    @GET("https://nmb.ovear.info/nmb-notice.json")
    suspend fun notice(): SaniouResult<NmbNotice>

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
    @GET("https://www.nmbxd1.com/Member/User/Index/verify.html")
    suspend fun getVerifyImage(): String

    /**
     * 用户登录
     */
    @POST("https://www.nmbxd1.com/Member/User/Index/login.html")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    /**
     * 获取用户饼干列表
     */
    @GET("https://www.nmbxd1.com/Member/User/Cookie/index.html")
    suspend fun getCookiesList(): CookieListResponse

    /**
     * 申请新饼干
     */
    @POST("https://www.nmbxd1.com/Member/User/Cookie/apply.html")
    suspend fun applyNewCookie(): String

    /**
     * 注册账号
     */
    @POST("https://www.nmbxd1.com/Member/User/Index/sendRegister.html")
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
    @POST("https://www.nmbxd1.com/Member/User/Index/sendForgotPassword.html")
    @FormUrlEncoded
    suspend fun resetPassword(
        @Field("email") email: String,
        @Field("verify") verify: String
    ): String
}


@OptIn(InternalAPI::class)
fun buildPartList(map: Map<String, Any?>): List<PartData> {
    val list = mutableListOf<PartData>()

    map.forEach { (key, value) ->
        when (value) {
            null -> Unit
            is String -> {
                list += PartData.FormItem(
                    value = value,
                    dispose = {},
                    partHeaders = Headers.build {
                        append(
                            HttpHeaders.ContentDisposition,
                            "form-data; name=\"$key\""
                        )
                    }
                )
            }

//            is File -> {
//                list += PartData.FileItem(
//                    { value.inputStream() },
//                    dispose = {},
//                    partHeaders = Headers.build {
//                        append(
//                            HttpHeaders.ContentDisposition,
//                            "form-data; name=\"$key\"; filename=\"${value.name}\""
//                        )
//                        append(HttpHeaders.ContentType, "application/octet-stream")
//                    }
//                )
//            }

            is ByteArray -> {
                list += PartData.BinaryItem(
                    { ByteReadChannel(value).readBuffer },
                    dispose = {},
                    partHeaders = Headers.build {
                        append(
                            HttpHeaders.ContentDisposition,
                            "form-data; name=\"$key\"; filename=\"$key.bin\""
                        )
                        append(HttpHeaders.ContentType, "application/octet-stream")
                    }
                )
            }

            else -> error("不支持的类型：${value::class}")
        }
    }

    return list
}
