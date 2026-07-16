package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseCreatePostResponse
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseUploadResponse
import ai.saniou.thread.domain.model.forum.PostAttachment
import ai.saniou.thread.domain.model.forum.PostDraft
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DiscoursePostingConnectorTest {
    @Test
    fun uploadsComposerAssetBeforePublishingMarkdownReference() = runBlocking {
        val transport = RecordingDiscoursePostTransport()
        val connector = DiscoursePostingConnector("forum", transport)

        val result = connector.createReply(
            topicId = "42",
            draft = PostDraft(
                content = "正文",
                attachment = PostAttachment("diagram.png", byteArrayOf(1, 2), "image/png"),
            ),
        )

        assertEquals("42", result.topicId)
        assertEquals(listOf("upload", "post"), transport.calls)
        assertEquals("正文\n\n![diagram.png](upload://asset)", transport.lastRaw)
    }

    @Test
    fun uploadFailurePreventsPostSideEffect() = runBlocking {
        val transport = RecordingDiscoursePostTransport(failUpload = true)
        val connector = DiscoursePostingConnector("forum", transport)

        assertFailsWith<IllegalStateException> {
            connector.createReply(
                "42",
                PostDraft(attachment = PostAttachment("bad.png", byteArrayOf(1), "image/png")),
            )
        }

        assertEquals(listOf("upload"), transport.calls)
        assertTrue(transport.lastRaw.isEmpty())
    }
}

private class RecordingDiscoursePostTransport(
    private val failUpload: Boolean = false,
) : DiscoursePostTransport {
    val calls = mutableListOf<String>()
    var lastRaw = ""

    override suspend fun upload(attachment: PostAttachment): DiscourseUploadResponse {
        calls += "upload"
        if (failUpload) error("upload failed")
        return DiscourseUploadResponse(
            id = 7,
            url = "/uploads/asset.png",
            shortUrl = "upload://asset",
            originalFilename = attachment.fileName,
        )
    }

    override suspend fun createPost(
        raw: String,
        title: String?,
        category: String?,
        topicId: String?,
    ): DiscourseCreatePostResponse {
        calls += "post"
        lastRaw = raw
        return DiscourseCreatePostResponse(id = 9, topicId = topicId?.toLong() ?: 8, postNumber = 2)
    }
}
