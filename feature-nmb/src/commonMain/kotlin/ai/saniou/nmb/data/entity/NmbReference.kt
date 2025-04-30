package ai.saniou.nmb.data.entity

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * 名称 	类型 	说明
 * id 	Number 	参见“查看版面”，不再重复
 * img 	String
 * ext 	String
 * now 	String
 * user_hash 	String
 * name 	String
 * title 	String
 * content 	String
 * sage 	Number
 * status 	String
 * Hide 	Number
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class NmbReference(
    val id: Long,
    val img: String,
    val ext: String,
    val now: String,
    @JsonNames("user_hash")
    val userHash: String,
    val name: String,
    val title: String,
    val content: String,
    val sage: Long,
    val status: String,
    @JsonNames("Hide")
    val hide: Long? = null,
)
