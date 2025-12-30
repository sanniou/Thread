package ai.saniou.thread.data.source.discourse.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscourseCategoriesResponse(
    @SerialName("category_list")
    val categoryList: DiscourseCategoryList,
)
