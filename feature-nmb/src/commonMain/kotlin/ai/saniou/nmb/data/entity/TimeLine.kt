package ai.saniou.nmb.data.entity

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class TimeLine(
    val id: Long,//时间线的 ID
    val name: String,//时间线名称
    @JsonNames("display_name")
    val displayName: String,//导航栏用的时间线名称？
    val notice: String,//时间线说明，使用 HTML，实际上在网页版也看不到
    @JsonNames("max_page")
    val maxPage: Long,//时间线页数上限，查看时间线时如果页数超过上限则只会返回最后一页的数据
)
