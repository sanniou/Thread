package ai.saniou.thread.data.source.social

import ai.saniou.thread.domain.model.social.CursorDirection
import ai.saniou.thread.domain.model.social.SocialCapabilities
import ai.saniou.thread.domain.model.social.SocialCursor
import ai.saniou.thread.domain.model.social.SocialIdentity
import ai.saniou.thread.domain.model.social.SocialInteraction
import ai.saniou.thread.domain.model.social.SocialMedia
import ai.saniou.thread.domain.model.social.SocialMediaKind
import ai.saniou.thread.domain.model.social.SocialPost
import ai.saniou.thread.domain.model.social.SocialTimelinePage
import ai.saniou.thread.domain.source.SocialConnector
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** Mastodon-compatible ActivityPub REST adapter; the UI only sees the open Social contract. */
class ActivityPubSocialConnector(
    override val sourceId: String,
    baseUrl: String,
    private val accessToken: String,
    private val client: HttpClient,
) : SocialConnector {
    private val baseUrl = baseUrl.trim().trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    override val capabilities = SocialCapabilities(
        interactions = setOf(SocialInteraction.LIKE, SocialInteraction.REPOST, SocialInteraction.BOOKMARK),
        supportsThreads = true,
        supportsContentWarnings = true,
        maxMediaItems = 4,
    )

    init {
        require(sourceId.isNotBlank())
        require(this.baseUrl.startsWith("https://") || this.baseUrl.startsWith("http://"))
        require(accessToken.isNotBlank()) { "ActivityPub home timeline requires an access token" }
    }

    override suspend fun timeline(cursor: SocialCursor?): Result<SocialTimelinePage> = runCatching {
        val response = client.get("$baseUrl/api/v1/timelines/home") {
            authenticated()
            parameter("limit", PAGE_SIZE)
            cursor?.let {
                parameter(if (it.direction == CursorDirection.OLDER) "max_id" else "min_id", it.value)
            }
        }
        check(response.status.isSuccess()) {
            "ActivityPub timeline failed: HTTP ${response.status.value} ${response.bodyAsText().take(240)}"
        }
        val statuses = json.decodeFromString<List<ActivityPubStatusDto>>(response.bodyAsText())
        SocialTimelinePage(
            items = statuses.map { it.toDomain(sourceId) },
            newerCursor = statuses.firstOrNull()?.id?.let { SocialCursor(it, CursorDirection.NEWER) },
            olderCursor = statuses.lastOrNull()?.id?.let { SocialCursor(it, CursorDirection.OLDER) },
        )
    }

    override suspend fun interact(
        postId: String,
        interaction: SocialInteraction,
        enabled: Boolean,
    ): Result<SocialPost> = runCatching {
        require(interaction in capabilities.interactions) { "$interaction is not supported by ActivityPub adapter" }
        val action = when (interaction) {
            SocialInteraction.LIKE -> if (enabled) "favourite" else "unfavourite"
            SocialInteraction.REPOST -> if (enabled) "reblog" else "unreblog"
            SocialInteraction.BOOKMARK -> if (enabled) "bookmark" else "unbookmark"
            SocialInteraction.REPLY, SocialInteraction.SHARE -> error("$interaction requires a separate system/composer action")
        }
        val response = client.post("$baseUrl/api/v1/statuses/$postId/$action") { authenticated() }
        check(response.status.isSuccess()) {
            "ActivityPub interaction failed: HTTP ${response.status.value} ${response.bodyAsText().take(240)}"
        }
        json.decodeFromString<ActivityPubStatusDto>(response.bodyAsText()).toDomain(sourceId)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authenticated() {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
    }

    private companion object {
        const val PAGE_SIZE = 40
    }
}

@Serializable
private data class ActivityPubStatusDto(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("in_reply_to_id") val inReplyToId: String? = null,
    val uri: String? = null,
    val url: String? = null,
    val content: String = "",
    @SerialName("spoiler_text") val spoilerText: String = "",
    val account: ActivityPubAccountDto,
    @SerialName("media_attachments") val mediaAttachments: List<ActivityPubMediaDto> = emptyList(),
    @SerialName("replies_count") val repliesCount: Long = 0,
    @SerialName("reblogs_count") val reblogsCount: Long = 0,
    @SerialName("favourites_count") val favouritesCount: Long = 0,
    val favourited: Boolean? = null,
    val reblogged: Boolean? = null,
    val bookmarked: Boolean? = null,
    val reblog: ActivityPubStatusDto? = null,
) {
    fun toDomain(sourceId: String): SocialPost {
        val target = reblog ?: this
        val active = buildSet {
            if (target.favourited == true) add(SocialInteraction.LIKE)
            if (target.reblogged == true) add(SocialInteraction.REPOST)
            if (target.bookmarked == true) add(SocialInteraction.BOOKMARK)
        }
        return SocialPost(
            id = id,
            sourceId = sourceId,
            author = SocialIdentity(
                id = target.account.id,
                displayName = target.account.displayName.ifBlank { target.account.username },
                handle = target.account.acct,
                avatarUrl = target.account.avatar,
                verified = target.account.fields.any { it.verifiedAt != null },
            ),
            body = target.content,
            createdAtEpochMillis = Instant.parse(createdAt).toEpochMilliseconds(),
            contentWarning = target.spoilerText.takeIf(String::isNotBlank),
            media = target.mediaAttachments.take(4).map(ActivityPubMediaDto::toDomain),
            interactionCounts = mapOf(
                SocialInteraction.REPLY to target.repliesCount,
                SocialInteraction.REPOST to target.reblogsCount,
                SocialInteraction.LIKE to target.favouritesCount,
            ),
            permittedInteractions = setOf(
                SocialInteraction.REPLY,
                SocialInteraction.REPOST,
                SocialInteraction.LIKE,
                SocialInteraction.BOOKMARK,
                SocialInteraction.SHARE,
            ),
            activeInteractions = active,
            canonicalUrl = url ?: uri ?: target.url ?: target.uri,
            replyToId = target.inReplyToId,
            repostOfId = reblog?.id,
        )
    }
}

@Serializable
private data class ActivityPubAccountDto(
    val id: String,
    val username: String,
    val acct: String? = null,
    @SerialName("display_name") val displayName: String = "",
    val avatar: String? = null,
    val fields: List<ActivityPubFieldDto> = emptyList(),
)

@Serializable
private data class ActivityPubFieldDto(
    @SerialName("verified_at") val verifiedAt: String? = null,
)

@Serializable
private data class ActivityPubMediaDto(
    val id: String,
    val type: String,
    val url: String,
    @SerialName("preview_url") val previewUrl: String? = null,
    val description: String? = null,
) {
    fun toDomain() = SocialMedia(
        id = id,
        kind = when (type.lowercase()) {
            "image" -> SocialMediaKind.IMAGE
            "video", "gifv" -> SocialMediaKind.VIDEO
            "audio" -> SocialMediaKind.AUDIO
            else -> SocialMediaKind.LINK
        },
        url = url,
        previewUrl = previewUrl,
        altText = description,
    )
}
