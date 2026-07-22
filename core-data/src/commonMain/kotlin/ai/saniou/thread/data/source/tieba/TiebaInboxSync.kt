package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.model.MessageListBean
import ai.saniou.thread.data.source.tieba.remote.NewTiebaApi
import ai.saniou.thread.domain.model.content.ContentReference
import ai.saniou.thread.domain.model.content.ContentReferenceKind
import ai.saniou.thread.domain.model.inbox.InboxEvent
import ai.saniou.thread.domain.model.inbox.InboxKind
import ai.saniou.thread.domain.repository.InboxRepository
import kotlin.time.Instant

/**
 * Pulls Tieba reply-me / at-me / agree-me feeds into the shared Inbox store.
 * Called from background refresh; safe to no-op when not logged in.
 */
class TiebaInboxSync(
    private val api: NewTiebaApi,
    private val inboxRepository: InboxRepository,
    private val parameterProvider: TiebaParameterProvider,
) {
    val sourceId: String = TiebaMapper.SOURCE_ID

    suspend fun refresh(maxPages: Int = 1): Int {
        if (parameterProvider.getBduss().isBlank()) return 0
        var upserted = 0
        for (page in 1..maxPages.coerceAtLeast(1)) {
            upserted += ingestReplyPage(page)
            upserted += ingestAtPage(page)
            upserted += ingestAgreePage(page)
        }
        return upserted
    }

    private suspend fun ingestReplyPage(page: Int): Int {
        val response = api.replyMe(page = page)
        response.ensureOk("回复我的")
        return response.replyList.orEmpty().sumOf { message ->
            upsertMessage(message, InboxKind.REPLY, kindLabel = "回复")
        }
    }

    private suspend fun ingestAtPage(page: Int): Int {
        val response = api.atMe(page = page)
        response.ensureOk("@我的")
        return response.atList.orEmpty().sumOf { message ->
            upsertMessage(message, InboxKind.MENTION, kindLabel = "@")
        }
    }

    private suspend fun ingestAgreePage(page: Int): Int {
        val response = api.agreeMe(page = page)
        response.ensureOk("点赞我的")
        // agreeMe reuses MessageListBean; list field varies by server — try both.
        val messages = response.replyList.orEmpty().ifEmpty { response.atList.orEmpty() }
        return messages.sumOf { message ->
            upsertMessage(message, InboxKind.SYSTEM, kindLabel = "赞了")
        }
    }

    private suspend fun upsertMessage(
        message: MessageListBean.MessageInfoBean,
        kind: InboxKind,
        kindLabel: String,
    ): Int {
        val threadId = message.threadId?.takeIf(String::isNotBlank) ?: return 0
        val postId = message.postId?.takeIf(String::isNotBlank)
        val eventId = buildString {
            append(sourceId)
            append(':')
            append(kind.name.lowercase())
            append(':')
            append(kindLabel.hashCode())
            append(':')
            append(threadId)
            append(':')
            append(postId ?: "0")
            append(':')
            append(message.time.orEmpty())
        }
        val actor = message.replyer?.nameShow?.takeIf(String::isNotBlank)
            ?: message.replyer?.name?.takeIf(String::isNotBlank)
            ?: "贴吧用户"
        val forum = message.forumName?.takeIf(String::isNotBlank)
        val title = buildString {
            append(actor)
            append(' ')
            append(kindLabel)
            if (!forum.isNullOrBlank()) {
                append(" · ")
                append(forum)
            }
        }
        val summary = message.content?.takeIf(String::isNotBlank)
            ?: message.quoteContent?.takeIf(String::isNotBlank)
            ?: message.title.orEmpty()
        val occurredAt = message.time?.toLongOrNull()?.let(Instant::fromEpochSeconds)
            ?: Instant.fromEpochSeconds(0)
        val reference = ContentReference(
            kind = if (postId != null) ContentReferenceKind.COMMENT else ContentReferenceKind.TOPIC,
            id = postId ?: threadId,
            sourceId = sourceId,
            parentId = postId?.let { threadId },
            canonicalUrl = "https://tieba.baidu.com/p/$threadId",
        )
        inboxRepository.upsert(
            InboxEvent(
                id = eventId,
                kind = kind,
                sourceId = sourceId,
                title = title.ifBlank { "贴吧消息" },
                summary = summary.ifBlank { message.title.orEmpty().ifBlank { "打开查看详情" } },
                reference = reference,
                occurredAt = occurredAt,
                readAt = if (message.unread == "0") occurredAt else null,
                muted = false,
                priority = when (kind) {
                    InboxKind.MENTION -> 1
                    InboxKind.SYSTEM -> 0
                    else -> 0
                },
            ),
        )
        return 1
    }
}

internal fun MessageListBean.ensureOk(label: String) {
    if (!errorCode.isNullOrBlank() && errorCode != "0") {
        throw IllegalStateException("贴吧$label 加载失败 ($errorCode)")
    }
}
