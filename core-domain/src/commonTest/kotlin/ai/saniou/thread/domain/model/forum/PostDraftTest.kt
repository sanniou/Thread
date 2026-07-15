package ai.saniou.thread.domain.model.forum

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PostDraftTest {
    @Test
    fun attachmentIsTransportAgnostic() {
        val bytes = byteArrayOf(1, 2, 3)
        val draft = PostDraft(
            content = "hello",
            attachment = PostAttachment(
                bytes = bytes,
                fileName = "image.png",
                contentType = "image/png",
            ),
        )

        assertEquals("hello", draft.content)
        assertEquals("image.png", draft.attachment?.fileName)
        assertContentEquals(bytes, draft.attachment?.bytes)
    }
}
