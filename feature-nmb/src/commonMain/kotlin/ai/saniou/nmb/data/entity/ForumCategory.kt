package ai.saniou.nmb.data.entity

import ai.saniou.nmb.db.table.Forum
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class ForumCategory(
    val id: Long, //版面分类的 ID
    val sort: Long, //版面分类的排序值，升序
    val name: String, //版面分类的名称
    val status: String, //原版 API 文档所述：始终为 n
    var forums: List<ForumDetail>
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class ForumDetail(
    val id: Long, //版面分类下每个版面的 ID
    @JsonNames("fgroup")
    val fGroup: Long = Long.MIN_VALUE, //版面所属的版面分类 ID，亚文化中时间线是一个错误的板块没有这个 id
    val sort: Long? = null, //版面在版面分类内的排序值
    val name: String, //版面名称
    val showName: String? = null, //导航栏用的版面名称，使用 HTML
    val msg: String, //版面说明，使用 HTML
    val interval: Long? = null, //发串的间隔时间，单位为秒
    @JsonNames("safe_mode")
    val safeMode: String? = null, //?
    @JsonNames("auto_delete")
    val autoDelete: Long? = null, //发串后被自动删除的时间，单位为小时，一般用于速报版，0 表示没有启用自动删除
    @JsonNames("thread_count")
    val threadCount: Long? = null, //版面内串的数量
    @JsonNames("permission_level")
    val permissionLevel: String? = null, //?
    @JsonNames("forum_fuse_id")
    val forumFuseId: String? = null, //?
    @JsonNames("createdAt")
    val createdAt: String? = null, //?
    @JsonNames("updateAt")
    val updateAt: String? = null, //?
    val status: String? = null, //原版 API 文档所述：始终为 n
)

fun ai.saniou.nmb.db.table.ForumCategory.toForumCategory(forums: List<ai.saniou.nmb.db.table.Forum>) =
    ForumCategory(
        id = id,
        sort = sort,
        name = name,
        status = status,
        forums = forums.map { it.toForumDetail() }
    )

fun ai.saniou.nmb.db.table.Forum.toForumDetail() = ForumDetail(
    id = id,
    fGroup = fGroup,
    sort = sort,
    name = name,
    showName = showName,
    msg = msg,
    interval = interval,
    safeMode = safeMode,
    autoDelete = autoDelete,
    threadCount = threadCount,
    permissionLevel = permissionLevel,
    forumFuseId = forumFuseId,
    status = status,
)

fun ForumCategory.toTable() = ai.saniou.nmb.db.table.ForumCategory(
    id = id,
    sort = sort,
    name = name,
    status = status
)

fun ForumDetail.toTable() = Forum(
    id = id,
    fGroup = fGroup,
    sort = sort,
    name = name,
    showName = showName,
    msg = msg,
    interval = interval,
    safeMode = safeMode,
    autoDelete = autoDelete,
    threadCount = threadCount,
    permissionLevel = permissionLevel,
    forumFuseId = forumFuseId,
    status = status,
)
