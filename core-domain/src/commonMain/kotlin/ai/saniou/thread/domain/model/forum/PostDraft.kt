package ai.saniou.thread.domain.model.forum

/**
 * Platform-neutral payload used by forum posting use cases.
 *
 * Network multipart types deliberately stay in the data layer. Platform image pickers only need
 * to turn their result into bytes before dispatching the draft to common business logic.
 */
data class PostDraft(
    val content: String = "",
    val name: String? = null,
    val title: String? = null,
    val water: Boolean = false,
    val attachment: PostAttachment? = null,
    /** Floor post id when composing a 楼中楼 / quoted reply (Tieba quote_id / repostid). */
    val quotePostId: String? = null,
    /** Target user id for the quoted floor (Tieba reply_uid). */
    val replyUserId: String? = null,
    /** Optional nested sub-post id when replying inside 楼中楼 (Tieba spid family). */
    val subPostId: String? = null,
)

class PostAttachment(
    val fileName: String,
    val bytes: ByteArray,
    val contentType: String = "application/octet-stream",
)
