package ai.saniou.thread.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class NmbReferenceParserTest {
    @Test
    fun keepsReferenceImagesAndNormalizesProtocolRelativeUrls() {
        val parsed = NmbReferenceParser.parse(
            html = """
                <a href="/t/42" class="h-threads-info-id">No.7</a>
                <span class="h-threads-info-title">标题</span>
                <span class="h-threads-info-email">无名氏</span>
                <span class="h-threads-info-uid">ID:abc</span>
                <span class="h-threads-info-createdat">2026-07-16(四)10:20:30</span>
                <div class="h-threads-content">正文</div>
                <a class="h-threads-img-a" href="//cdn.example.test/image/7.jpg">image</a>
            """.trimIndent(),
            refId = 7,
        )

        assertEquals("42", parsed.comment.topicId)
        assertEquals("https://cdn.example.test/image/7.jpg", parsed.images.single().originalUrl)
        assertEquals("jpg", parsed.images.single().extension)
    }
}
