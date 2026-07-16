package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.corecommon.utils.toTime
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.ImageType
import ai.saniou.thread.domain.repository.ReferenceRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
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
            .mapToOneOrNull(ioDispatcher)
            .mapNotNull { it?.toDomain(db.imageQueries, db.commentQueries) }
            .onStart {
                if (db.commentQueries.getCommentById("nmb", id.toString())
                        .executeAsOneOrNull() == null
                ) {
                    val html = api.refHtml(id)
                    val parsed = NmbReferenceParser.parse(html, id)
                    db.transaction {
                        db.commentQueries.upsertComment(parsed.comment)
                        db.imageQueries.deleteImagesByParent("nmb", parsed.comment.id, ImageType.Comment)
                        parsed.images.forEachIndexed { index, image ->
                            db.imageQueries.upsertImage(
                                image.toEntity("nmb", parsed.comment.id, ImageType.Comment, index.toLong())
                            )
                        }
                    }
                }
            }
            .flowOn(ioDispatcher)

}

internal data class ParsedNmbReference(
    val comment: ai.saniou.thread.db.table.forum.Comment,
    val images: List<Image>,
)

internal object NmbReferenceParser {
    @OptIn(ExperimentalTime::class)
    fun parse(
        html: String,
        refId: Long,
    ): ParsedNmbReference {
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

        val images = imgRegex.findAll(html).map { match ->
            val url = normalizeUrl(match.groupValues[1])
            Image(
                originalUrl = url,
                thumbnailUrl = url,
                extension = url.substringBefore('?').substringAfterLast('.', "").takeIf(String::isNotBlank),
            )
        }.distinctBy(Image::originalUrl).toList()

        val comment = ai.saniou.thread.db.table.forum.Comment(
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
            floor = Long.MIN_VALUE,
            replyToId = null,
            agreeCount = 0,
            disagreeCount = 0,
            subCommentCount = 0,
            authorLevel = null,
            isPo = false
        )
        return ParsedNmbReference(comment, images)
    }

    private fun normalizeUrl(url: String): String = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith('/') -> "https://www.nmbxd1.com$url"
        else -> url
    }
}
