package ai.saniou.thread.data.repository

import ai.saniou.thread.db.Database
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.corecommon.utils.toTime
import ai.saniou.thread.domain.model.forum.ThreadReply
import ai.saniou.thread.domain.repository.ReferenceRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlin.time.ExperimentalTime

class ReferenceRepositoryImpl(
    private val api: NmbXdApi,
    private val db: Database,
) : ReferenceRepository {

    override fun getReference(id: Long): Flow<ThreadReply> =
        db.threadReplyQueries.getThreadReplyById("nmb", id.toString())
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .mapNotNull { it?.toDomain() }
            .onStart {
                if (db.threadReplyQueries.getThreadReplyById("nmb", id.toString())
                        .executeAsOneOrNull() == null
                ) {
                    val html = api.refHtml(id)
                    val reply = parseRefHtml(html, id)
                    db.threadReplyQueries.upsertThreadReply(reply)
                }
            }
            .flowOn(Dispatchers.IO)

    @OptIn(ExperimentalTime::class)
    private fun parseRefHtml(
        html: String,
        refId: Long,
    ): ai.saniou.thread.db.table.forum.ThreadReply {
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

        return ai.saniou.thread.db.table.forum.ThreadReply(
            id = parsedId.toString(),
            userHash = userHash,
            admin = if (isAdmin) 1L else 0L,
            title = title,
            now = now,
            content = content,
            img = img,
            ext = ext,
            name = name,
            threadId = threadId.toString(),
            sourceId = "nmb",
            page = Long.MIN_VALUE
        )
    }
}
