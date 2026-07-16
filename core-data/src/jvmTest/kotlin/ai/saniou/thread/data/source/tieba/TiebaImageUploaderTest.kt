package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.model.UploadPictureResultBean
import ai.saniou.thread.domain.model.forum.PostAttachment
import io.ktor.client.request.forms.MultiPartFormDataContent
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class TiebaImageUploaderTest {
    @Test
    fun uploadsAllChunksAndReturnsOfficialReplyMarkup() = runBlocking {
        val requests = mutableListOf<MultiPartFormDataContent>()
        val uploader = TiebaImageUploader(
            client = TiebaPictureUploadClient { body ->
                requests += body
                response("pic-7", 1, 1)
            },
            chunkSize = 10,
        )

        val uploaded = uploader.upload(
            PostAttachment("tiny.png", png(width = 3, height = 5), "image/png"),
            forumName = "测试吧",
        )

        assertEquals(3, requests.size)
        assertEquals("#(pic,pic-7,1,1)", uploaded.markup)
    }

    @Test
    fun readsGifDimensionsInCommonCode() {
        val gif = "GIF89a".encodeToByteArray() + byteArrayOf(0x20, 0x00, 0x10, 0x00)
        assertEquals(ImageDimensions(32, 16), ImageDimensions.read(gif))
    }

    private fun png(width: Int, height: Int): ByteArray = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0, 0, 0, 13, 0x49, 0x48, 0x44, 0x52,
        0, 0, 0, width.toByte(), 0, 0, 0, height.toByte(),
    )

    private fun response(picId: String, width: Int, height: Int) = UploadPictureResultBean(
        errorCode = "0",
        errorMsg = "",
        resourceId = "resource",
        chunkNo = "1",
        picId = picId,
        picInfo = UploadPictureResultBean.PicInfo(
            originPic = item(width, height),
            bigPic = item(width, height),
            smallPic = item(width, height),
        ),
    )

    private fun item(width: Int, height: Int) = UploadPictureResultBean.PicInfoItem(
        width = width.toString(),
        height = height.toString(),
        type = "png",
        picUrl = "https://example.test/image.png",
    )
}
