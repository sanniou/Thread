package ai.saniou.nmb.domain

import ai.saniou.thread.data.source.nmb.remote.dto.ThreadReply
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.data.source.nmb.remote.dto.toThreadReply
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.db.Database

class GetReferenceUseCase(
    private val forumRepository: ForumRepository,
    private val db: Database,
) {
    suspend operator fun invoke(id: Long): Result<ThreadReply> {
        // 尝试从数据库获取
        val localReply = db.threadReplyQueries.getThreadReplyById(id).executeAsOneOrNull()?.toThreadReply()
        if (localReply != null) {
            return Result.success(localReply)
        }

        // 如果数据库没有，则从网络获取
        return try {
            val html = forumRepository.refHtml(id)
            val reply = parseRefHtml(html, id)
            // 存入数据库，注意 threadId 设为特殊值以作区分
            val threadIdForDb = if (reply.threadId > 0) reply.threadId else Long.MIN_VALUE
            db.threadReplyQueries.upsertThreadReply(reply.toTable(threadIdForDb))
            Result.success(reply)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRefHtml(html: String, refId: Long): ThreadReply {
        val idRegex = """href="([^"]*)"[^>]*class="h-threads-info-id">No\.(\d+)""".toRegex()
        val titleRegex = """<span class="h-threads-info-title">(.*?)</span>""".toRegex()
        val nameRegex = """<span class="h-threads-info-email"[^>]*>(.*?)</span>""".toRegex()
        val uidRegex = """<span class="h-threads-info-uid">ID:(.*?)</span>""".toRegex()
        val timeRegex = """<span class="h-threads-info-createdat">(.*?)</span>""".toRegex()
        val contentRegex = """<div class="h-threads-content">([\s\S]*?)</div>""".toRegex()
        val imgRegex = """<a[^>]*class="h-threads-img-a"[^>]*href="([^"]+)"""".toRegex()

        val idMatch = idRegex.find(html)
        val href = idMatch?.groupValues?.get(1) ?: ""
        val parsedId = idMatch?.groupValues?.get(2)?.toLongOrNull() ?: refId

        var threadId: Long = 0
        if (href.contains("/t/")) {
            val tidMatch = """/t/(\d+)""".toRegex().find(href)
            threadId = tidMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0
        }

        val title = titleRegex.find(html)?.groupValues?.get(1) ?: "无标题"
        val name = nameRegex.find(html)?.groupValues?.get(1) ?: "无名氏"

        val uidContent = uidRegex.find(html)?.groupValues?.get(1) ?: ""
        val isAdmin = uidContent.contains("color=\"red\"") || uidContent.contains("Admin")
        // Remove HTML tags from userHash if present
        val userHash = if (isAdmin) "Admin" else uidContent.replace(Regex("<[^>]*>"), "")

        val now = timeRegex.find(html)?.groupValues?.get(1) ?: ""
        val content = contentRegex.find(html)?.groupValues?.get(1)?.trim() ?: ""

        var img = ""
        var ext = ""
        val imgMatch = imgRegex.find(html)
        if (imgMatch != null) {
            val imgUrl = imgMatch.groupValues[1]
            if (imgUrl.contains("/image/")) {
                val parts = imgUrl.split("/image/")
                if (parts.size > 1) {
                    val filename = parts[1]
                    val dotIndex = filename.lastIndexOf('.')
                    if (dotIndex != -1) {
                        img = filename.substring(0, dotIndex)
                        ext = filename.substring(dotIndex)
                    }
                }
            }
        }

        return ThreadReply(
            id = parsedId,
            userHash = userHash,
            admin = if (isAdmin) 1L else 0L,
            title = title,
            now = now,
            content = content,
            img = img,
            ext = ext,
            name = name,
            threadId = threadId
        )
    }
}
