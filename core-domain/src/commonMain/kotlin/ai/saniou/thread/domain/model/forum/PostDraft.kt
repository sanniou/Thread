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
)

class PostAttachment(
    val fileName: String,
    val bytes: ByteArray,
    val contentType: String = "application/octet-stream",
)
