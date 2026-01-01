package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HotTopicForumBean(
    @SerialName("forum_list")
    val forumList: ForumListBean,
    @SerialName("pk_info")
    val pkInfo: PkInfoBean
) {
    @Serializable
    data class ForumListBean(
        val output: List<ForumBean>
    )

    @Serializable
    data class ForumBean(
        val dummy: String? = null
    )

    @Serializable
    data class PkInfoBean(
        val ret: List<PkInfoRetBean>
    )

    @Serializable
    data class PkInfoRetBean(
        @SerialName("create_time")
        val createTime: String,
        @SerialName("module_name")
        val moduleName: String,
        @SerialName("module_type")
        val moduleType: String,
        val pics: PkPicBean,
        @SerialName("pic_urls")
        val picUrls: PkPicBean,
        @SerialName("has_selected")
        val hasSelected: Boolean,
        @SerialName("num_coefficient")
        val numCoefficient: String,
        @SerialName("pk_desc_1")
        val pkDesc1: String,
        @SerialName("pk_desc_2")
        val pkDesc2: String,
        @SerialName("pk_desc_3")
        val pkDesc3: String,
        @SerialName("pk_desc_4")
        val pkDesc4: String,
        @SerialName("pk_id")
        val pkId: String,
        @SerialName("pk_num_1")
        val pkNum1: String,
        @SerialName("pk_num_2")
        val pkNum2: String,
        @SerialName("pk_num_3")
        val pkNum3: String,
        @SerialName("pk_num_4")
        val pkNum4: String,
        @SerialName("selected_index")
        val selectedIndex: String,
        val title: String,
        @SerialName("topic_id")
        val topicId: String
    )

    @Serializable
    data class PkPicBean(
        @SerialName("pk_icon_1")
        val pkIcon1: String,
        @SerialName("pk_icon_2")
        val pkIcon2: String,
        @SerialName("pk_icon_after_1")
        val pkIconAfter1: String,
        @SerialName("pk_icon_after_2")
        val pkIconAfter2: String
    )
}
