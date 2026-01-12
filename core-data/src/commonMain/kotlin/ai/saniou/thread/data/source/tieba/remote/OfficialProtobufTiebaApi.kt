package ai.saniou.thread.data.source.tieba.remote

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.POST
import io.ktor.client.request.forms.MultiPartFormDataContent
import kotlinx.coroutines.flow.Flow
import com.huanchengfly.tieba.post.api.models.protos.addPost.AddPostResponse
import com.huanchengfly.tieba.post.api.models.protos.forumRecommend.ForumRecommendResponse
import com.huanchengfly.tieba.post.api.models.protos.forumRuleDetail.ForumRuleDetailResponse
import com.huanchengfly.tieba.post.api.models.protos.frsPage.FrsPageResponse
import com.huanchengfly.tieba.post.api.models.protos.getBawuInfo.GetBawuInfoResponse
import com.huanchengfly.tieba.post.api.models.protos.getForumDetail.GetForumDetailResponse
import com.huanchengfly.tieba.post.api.models.protos.getHistoryForum.GetHistoryForumResponse
import com.huanchengfly.tieba.post.api.models.protos.getLevelInfo.GetLevelInfoResponse
import com.huanchengfly.tieba.post.api.models.protos.getMemberInfo.GetMemberInfoResponse
import com.huanchengfly.tieba.post.api.models.protos.getUserInfo.GetUserInfoResponse
import com.huanchengfly.tieba.post.api.models.protos.hotThreadList.HotThreadListResponse
import com.huanchengfly.tieba.post.api.models.protos.pbFloor.PbFloorResponse
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponse
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedResponse
import com.huanchengfly.tieba.post.api.models.protos.profile.ProfileResponse
import com.huanchengfly.tieba.post.api.models.protos.searchSug.SearchSugResponse
import com.huanchengfly.tieba.post.api.models.protos.threadList.ThreadListResponse
import com.huanchengfly.tieba.post.api.models.protos.topicList.TopicListResponse
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeResponse
import com.huanchengfly.tieba.post.api.models.protos.userPost.UserPostResponse

interface OfficialProtobufTiebaApi {
    /**
     * 个性化推荐流 (Protobuf)
     * cmd=309264
     */
    @POST("c/f/excellent/personalized?cmd=309264")
    suspend fun personalizedFlow(
        @Body body: MultiPartFormDataContent,
    ): PersonalizedResponse

    /**
     * 用户关注的吧更新流 (Protobuf)
     * cmd=309474
     */
    @POST("c/f/concern/userlike?cmd=309474")
    suspend fun userLikeFlow(
        @Body body: MultiPartFormDataContent,
    ): UserLikeResponse

    /**
     * 热门帖子列表 (Protobuf)
     * cmd=309661
     */
    @POST("c/f/forum/hotThreadList?cmd=309661")
    suspend fun hotThreadListFlow(
        @Body body: MultiPartFormDataContent,
    ): HotThreadListResponse

    /**
     * 推荐话题列表 (Protobuf)
     * cmd=309289
     */
    @POST("c/f/recommend/topicList?cmd=309289")
    suspend fun topicListFlow(
        @Body body: MultiPartFormDataContent,
    ): TopicListResponse

    /**
     * 板块推荐 (Protobuf)
     * cmd=303011
     */
    @POST("c/f/forum/forumrecommend?cmd=303011")
    suspend fun forumRecommendFlow(
        @Body body: MultiPartFormDataContent,
    ): ForumRecommendResponse

    /**
     * 吧首页帖子列表 (FRS Page) (Protobuf)
     * cmd=301001
     * @param body 请求体 (PbFrsPageReq)
     * @param forumName 吧名 header (部分请求需要)
     */
    @POST("c/f/frs/page?cmd=301001")
    suspend fun frsPageFlow(
        @Body body: MultiPartFormDataContent,
        @Header("forum_name") forumName: String? = null,
    ): FrsPageResponse

    /**
     * 吧帖子列表 (FRS ThreadList) (Protobuf)
     * cmd=301002
     */
    @POST("c/f/frs/threadlist?cmd=301002")
    suspend fun threadListFlow(
        @Body body: MultiPartFormDataContent,
    ): ThreadListResponse

    /**
     * 用户资料 (Protobuf)
     * cmd=303012
     */
    @POST("c/u/user/profile?cmd=303012&format=protobuf")
    suspend fun profileFlow(
        @Body body: MultiPartFormDataContent,
    ): ProfileResponse

    /**
     * 帖子详情页 (PB Page) (Protobuf)
     * cmd=302001
     */
    @POST("c/f/pb/page?cmd=302001&format=protobuf")
    suspend fun pbPageFlow(
        @Body body: MultiPartFormDataContent,
    ): PbPageResponse

    /**
     * 楼中楼 (PB Floor) (Protobuf)
     * cmd=302002
     */
    @POST("c/f/pb/floor?cmd=302002&format=protobuf")
    suspend fun pbFloorFlow(
        @Body body: MultiPartFormDataContent,
    ): PbFloorResponse

    /**
     * 发布新帖 (Protobuf)
     * cmd=309731
     */
    @POST("c/c/post/add?cmd=309731&format=protobuf")
    suspend fun addPostFlow(
        @Body body: MultiPartFormDataContent,
    ): AddPostResponse

    /**
     * 搜索建议 (Protobuf)
     * cmd=309438
     */
    @POST("c/s/searchSug?cmd=309438&format=protobuf")
    suspend fun searchSugFlow(
        @Body body: MultiPartFormDataContent,
    ): SearchSugResponse

    /**
     * 获取吧详情 (Protobuf)
     * cmd=303021
     */
    @POST("c/f/forum/getforumdetail?cmd=303021&format=protobuf")
    suspend fun getForumDetailFlow(
        @Body body: MultiPartFormDataContent,
    ): GetForumDetailResponse

    /**
     * 获取吧务信息 (Protobuf)
     * cmd=301007
     */
    @POST("c/f/forum/getBawuInfo?cmd=301007&format=protobuf")
    suspend fun getBawuInfoFlow(
        @Body body: MultiPartFormDataContent,
    ): GetBawuInfoResponse

    /**
     * 获取等级信息 (Protobuf)
     * cmd=301005
     */
    @POST("c/f/forum/getLevelInfo?cmd=301005&format=protobuf")
    suspend fun getLevelInfoFlow(
        @Body body: MultiPartFormDataContent,
    ): GetLevelInfoResponse

    /**
     * 获取成员信息 (Protobuf)
     * cmd=301004
     */
    @POST("c/f/forum/getMemberInfo?cmd=301004&format=protobuf")
    suspend fun getMemberInfoFlow(
        @Body body: MultiPartFormDataContent,
    ): GetMemberInfoResponse

    /**
     * 获取吧规详情 (Protobuf)
     * cmd=309690
     */
    @POST("c/f/forum/forumRuleDetail?cmd=309690&format=protobuf")
    suspend fun forumRuleDetailFlow(
        @Body body: MultiPartFormDataContent,
    ): ForumRuleDetailResponse

    /**
     * 获取用户发布列表 (Protobuf)
     * cmd=303002
     */
    @POST("c/u/feed/userpost?cmd=303002&format=protobuf")
    suspend fun userPostFlow(
        @Body body: MultiPartFormDataContent,
    ): UserPostResponse

    /**
     * 获取用户信息 (Protobuf)
     * cmd=303024
     */
    @POST("c/u/user/getuserinfo?cmd=303024&format=protobuf")
    suspend fun getUserInfoFlow(
        @Body body: MultiPartFormDataContent,
    ): GetUserInfoResponse

    /**
     * 获取浏览历史板块 (Protobuf)
     * cmd=309601
     */
    @POST("c/f/forum/gethistoryforum?cmd=309601&format=protobuf")
    suspend fun getHistoryForumFlow(
        @Body body: MultiPartFormDataContent,
    ): GetHistoryForumResponse
}
