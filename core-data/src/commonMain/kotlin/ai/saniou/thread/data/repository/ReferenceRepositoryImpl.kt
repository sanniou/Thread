package ai.saniou.thread.data.repository

import ai.saniou.corecommon.utils.toTime
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Comment
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

    override fun getReference(id: Long): Flow<Comment> =
        db.commentQueries.getCommentById("nmb", id.toString())
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .mapNotNull { it?.toDomain(db.imageQueries) }
            .onStart {
                if (db.commentQueries.getCommentById("nmb", id.toString())
                        .executeAsOneOrNull() == null
                ) {
                    val html = api.refHtml(id)
                    val reply = parseRefHtml(html, id)
                    db.commentQueries.upsertComment(reply)
                }
            }
            .flowOn(Dispatchers.IO)

    @OptIn(ExperimentalTime::class)
    private fun parseRefHtml(
        html: String,
        refId: Long,
    ): ai.saniou.thread.db.table.forum.Comment {
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

        // TODO: Handle image parsing and saving to Image table
        // For now, we only populate the fields available in the new Comment table
        // img/ext columns are removed from Comment table

        return ai.saniou.thread.db.table.forum.Comment(
            id = parsedId.toString(),
            sourceId = "nmb",
            topicId = threadId.toString(),
            page = Long.MIN_VALUE,
            userHash = userHash,
            admin = if (isAdmin) 1L else 0L,
            title = title,
            createdAt = now.toTime().toEpochMilliseconds(),
            content = content,
            // img/ext removed
            authorName = name,
            floor = null,
            replyToId = null,
            agreeCount = 0,
            disagreeCount = 0,
            subCommentCount = 0,
            authorLevel = null,
            isPo = false
        )
    }
}
