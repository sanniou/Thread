package ai.saniou.thread.data.source.tieba

import ai.saniou.corecommon.utils.toMD5
import ai.saniou.thread.data.source.tieba.model.UploadPictureResultBean
import ai.saniou.thread.domain.model.forum.PostAttachment
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

internal fun interface TiebaPictureUploadClient {
    suspend fun upload(body: MultiPartFormDataContent): UploadPictureResultBean
}

internal data class TiebaUploadedImage(
    val picId: String,
    val width: Int,
    val height: Int,
) {
    val markup: String = "#(pic,$picId,$width,$height)"
}

/**
 * Common Tieba chunk protocol. It deliberately works on [ByteArray] so every platform picker can
 * share the same upload transaction without Android bitmap or filesystem dependencies.
 */
internal class TiebaImageUploader(
    private val client: TiebaPictureUploadClient,
    private val chunkSize: Int = DEFAULT_CHUNK_SIZE,
) {
    suspend fun upload(attachment: PostAttachment, forumName: String): TiebaUploadedImage {
        require(attachment.bytes.isNotEmpty()) { "图片不能为空" }
        require(attachment.bytes.size <= MAX_IMAGE_BYTES) { "贴吧图片不能超过 10 MB" }
        val dimensions = ImageDimensions.read(attachment.bytes)
            ?: throw IllegalArgumentException("无法识别图片尺寸，仅支持 PNG/JPEG/GIF/WebP")
        val resourceId = "${attachment.bytes.toMD5()}$chunkSize"
        val chunks = attachment.bytes.asList().chunked(chunkSize)
        var result: UploadPictureResultBean? = null

        chunks.forEachIndexed { index, values ->
            val bytes = values.toByteArray()
            result = client.upload(
                MultiPartFormDataContent(
                    formData {
                        append("alt", "json")
                        append("chunkNo", (index + 1).toString())
                        if (forumName.isNotBlank()) append("forum_name", forumName)
                        append("groupId", "1")
                        append("height", dimensions.height.toString())
                        append("isFinish", (index == chunks.lastIndex).toString())
                        append("is_bjh", "0")
                        append("pic_water_type", "2")
                        append("resourceId", resourceId)
                        append("saveOrigin", "false")
                        append("size", attachment.bytes.size.toString())
                        if (forumName.isNotBlank()) append("small_flow_fname", forumName)
                        append("width", dimensions.width.toString())
                        append(
                            key = "chunk",
                            value = bytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, "application/octet-stream")
                                append(HttpHeaders.ContentDisposition, "filename=\"file\"")
                            },
                        )
                    },
                    boundary = BOUNDARY,
                )
            ).also { response ->
                if (response.errorCode.isNotBlank() && response.errorCode != "0") {
                    throw IllegalStateException(
                        response.errorMsg.ifBlank { "贴吧图片上传失败 (${response.errorCode})" }
                    )
                }
            }
        }

        val uploaded = requireNotNull(result) { "贴吧图片上传没有返回结果" }
        val origin = uploaded.picInfo.originPic
        return TiebaUploadedImage(
            picId = uploaded.picId,
            width = origin.width.toIntOrNull() ?: dimensions.width,
            height = origin.height.toIntOrNull() ?: dimensions.height,
        )
    }

    private companion object {
        const val DEFAULT_CHUNK_SIZE = 512_000
        const val MAX_IMAGE_BYTES = 10 * 1024 * 1024
        const val BOUNDARY = "--------7da3d81520810*"
    }
}

internal data class ImageDimensions(val width: Int, val height: Int) {
    companion object {
        fun read(bytes: ByteArray): ImageDimensions? = when {
            bytes.isPng() -> dimensions(bytes.u32be(16), bytes.u32be(20))
            bytes.isGif() -> dimensions(bytes.u16le(6), bytes.u16le(8))
            bytes.isJpeg() -> readJpeg(bytes)
            bytes.isWebP() -> readWebP(bytes)
            else -> null
        }

        private fun dimensions(width: Int, height: Int) =
            if (width > 0 && height > 0) ImageDimensions(width, height) else null

        private fun readJpeg(bytes: ByteArray): ImageDimensions? {
            var offset = 2
            while (offset + 8 < bytes.size) {
                if (bytes[offset].u8() != 0xFF) return null
                val marker = bytes[offset + 1].u8()
                offset += 2
                if (marker == 0xD8 || marker == 0xD9) continue
                if (offset + 2 > bytes.size) return null
                val length = bytes.u16be(offset)
                if (length < 2 || offset + length > bytes.size) return null
                if (marker in SOF_MARKERS) {
                    return dimensions(bytes.u16be(offset + 3), bytes.u16be(offset + 5))
                }
                offset += length
            }
            return null
        }

        private fun readWebP(bytes: ByteArray): ImageDimensions? {
            val type = bytes.ascii(12, 4)
            return when (type) {
                "VP8 " -> if (bytes.size >= 30) dimensions(bytes.u16le(26) and 0x3FFF, bytes.u16le(28) and 0x3FFF) else null
                "VP8L" -> if (bytes.size >= 25) {
                    val b1 = bytes[21].u8()
                    val b2 = bytes[22].u8()
                    val b3 = bytes[23].u8()
                    val b4 = bytes[24].u8()
                    dimensions(1 + b1 + ((b2 and 0x3F) shl 8), 1 + (b2 shr 6) + (b3 shl 2) + ((b4 and 0x0F) shl 10))
                } else null
                "VP8X" -> if (bytes.size >= 30) dimensions(1 + bytes.u24le(24), 1 + bytes.u24le(27)) else null
                else -> null
            }
        }

        private val SOF_MARKERS = setOf(0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF)
    }
}

private fun ByteArray.isPng() = size >= 24 && u32be(0) == 0x89504E47.toInt() && ascii(12, 4) == "IHDR"
private fun ByteArray.isGif() = size >= 10 && (ascii(0, 6) == "GIF87a" || ascii(0, 6) == "GIF89a")
private fun ByteArray.isJpeg() = size >= 4 && this[0].u8() == 0xFF && this[1].u8() == 0xD8
private fun ByteArray.isWebP() = size >= 30 && ascii(0, 4) == "RIFF" && ascii(8, 4) == "WEBP"
private fun ByteArray.ascii(offset: Int, length: Int) = (offset until offset + length).map { this[it].toInt().toChar() }.joinToString("")
private fun ByteArray.u16be(offset: Int) = (this[offset].u8() shl 8) or this[offset + 1].u8()
private fun ByteArray.u16le(offset: Int) = this[offset].u8() or (this[offset + 1].u8() shl 8)
private fun ByteArray.u24le(offset: Int) = u16le(offset) or (this[offset + 2].u8() shl 16)
private fun ByteArray.u32be(offset: Int) = (u16be(offset) shl 16) or u16be(offset + 2)
private fun Byte.u8() = toInt() and 0xFF
