package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.remote.ClientVersion
import ai.saniou.thread.data.source.tieba.remote.OfficialProtobufTiebaApi
import ai.saniou.thread.data.source.tieba.remote.TiebaProtoBuilder
import ai.saniou.thread.domain.model.forum.ForumRuleDetail
import ai.saniou.thread.domain.model.forum.ForumRuleItem
import com.huanchengfly.tieba.post.api.models.protos.forumRuleDetail.ForumRuleDetailRequest
import com.huanchengfly.tieba.post.api.models.protos.forumRuleDetail.ForumRuleDetailRequestData

/**
 * Read-only forum rules via official protobuf forumRuleDetail.
 */
class TiebaForumRuleService(
    private val protobufApi: OfficialProtobufTiebaApi,
    private val parameterProvider: TiebaParameterProvider,
) {
    val sourceId: String = TiebaMapper.SOURCE_ID

    suspend fun load(channelId: String): ForumRuleDetail {
        val forumId = channelId.toLongOrNull()
            ?: throw IllegalArgumentException("无效的贴吧 ID: $channelId")
        val body = TiebaProtoBuilder.buildProtobufFormBody(
            data = ForumRuleDetailRequest(
                data_ = ForumRuleDetailRequestData(forum_id = forumId),
            ),
            clientVersion = ClientVersion.TIEBA_V11,
            parameterProvider = parameterProvider,
        )
        val response = protobufApi.forumRuleDetailFlow(body)
        val errorCode = response.error?.error_code
        if (errorCode != null && errorCode != 0) {
            throw IllegalStateException(
                response.error?.error_msg?.takeIf(String::isNotBlank)
                    ?: "加载吧规失败 ($errorCode)",
            )
        }
        val data = response.data_ ?: throw IllegalStateException("吧规数据为空")
        return ForumRuleDetail(
            channelId = channelId,
            title = data.title.ifBlank { "吧规" },
            preface = data.preface,
            publishTime = data.publish_time,
            rules = data.rules.map { rule ->
                ForumRuleItem(
                    title = rule.title.ifBlank { "规则" },
                    content = rule.content
                        .mapNotNull { block -> block.text.takeIf(String::isNotBlank) }
                        .joinToString("\n")
                        .trim(),
                )
            }.filter { it.title.isNotBlank() || it.content.isNotBlank() },
        )
    }
}
