package ai.saniou.nmb.data.entity

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class ForumCategory(
    val id: String, //版面分类的 ID
    val sort: String, //版面分类的排序值，升序
    val name: String, //版面分类的名称
    val status: String, //原版 API 文档所述：始终为 n
    val forums: List<ForumDetail>
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class ForumDetail(
    val id: String, //版面分类下每个版面的 ID
    val fgroup: String, //版面所属的版面分类 ID
    val sort: String, //版面在版面分类内的排序值
    val name: String, //版面名称
    val showName: String, //导航栏用的版面名称，使用 HTML
    val msg: String, //版面说明，使用 HTML
    val interval: String, //发串的间隔时间，单位为秒
    @JsonNames("safe_mode")
    val safeMode: String, //?
    @JsonNames("auto_delete")
    val autoDelete: String, //发串后被自动删除的时间，单位为小时，一般用于速报版，0 表示没有启用自动删除
    @JsonNames("thread_count")
    val threadCount: String, //版面内串的数量
    @JsonNames("permission_level")
    val permissionLevel: String, //?
    @JsonNames("forum_fuse_id")
    val forumFuseId: String, //?
    @JsonNames("createdAt")
    val createdAt: String, //?
    @JsonNames("updateAt")
    val updateAt: String, //?
    val status: String, //原版 API 文档所述：始终为 n
)

