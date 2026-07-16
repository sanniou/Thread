package ai.saniou.thread.data.reader

import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.model.reader.FeedType
import ai.saniou.thread.domain.model.reader.ReaderSubscriptionFormat
import com.fleeksoft.ksoup.Ksoup
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ReaderSubscriptionCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(sources: List<FeedSource>, format: ReaderSubscriptionFormat): String = when (format) {
        ReaderSubscriptionFormat.JSON -> json.encodeToString(
            ReaderSubscriptionBundle(sources = sources.map(FeedSubscriptionSnapshot::fromDomain)),
        )
        ReaderSubscriptionFormat.OPML -> buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<opml version=\"2.0\">")
            appendLine("  <head><title>Thread subscriptions</title></head>")
            appendLine("  <body>")
            sources.forEach { source ->
                append("    <outline type=\"rss\" text=\"")
                append(source.name.escapeXml())
                append("\" title=\"")
                append(source.name.escapeXml())
                append("\" xmlUrl=\"")
                append(source.url.escapeXml())
                append("\" threadId=\"")
                append(source.id.escapeXml())
                append("\" autoRefresh=\"")
                append(source.autoRefresh)
                append("\" refreshInterval=\"")
                append(source.refreshInterval)
                appendLine("\" />")
            }
            appendLine("  </body>")
            append("</opml>")
        }
    }

    fun decode(payload: String, format: ReaderSubscriptionFormat): List<FeedSource> {
        require(payload.isNotBlank()) { "Subscription payload must not be blank" }
        val sources = when (format) {
            ReaderSubscriptionFormat.JSON ->
                json.decodeFromString<ReaderSubscriptionBundle>(payload).sources.map { it.toDomain() }
            ReaderSubscriptionFormat.OPML -> decodeOpml(payload)
        }
        require(sources.isNotEmpty()) { "No subscriptions found" }
        require(sources.distinctBy { it.id }.size == sources.size) { "Duplicate subscription id" }
        require(sources.distinctBy { it.url }.size == sources.size) { "Duplicate subscription URL" }
        return sources.onEach(::validate)
    }

    private fun decodeOpml(payload: String): List<FeedSource> =
        Ksoup.parse(payload).select("outline").mapNotNull { outline ->
            val url = (outline.attr("xmlUrl").ifBlank { outline.attr("xmlurl") })
                .takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = outline.attr("title").ifBlank { outline.attr("text") }.ifBlank { url }
            val id = outline.attr("threadId").ifBlank { outline.attr("threadid") }
                .ifBlank { stableId(url) }
            FeedSource(
                id = id,
                name = name,
                url = url,
                type = FeedType.RSS,
                autoRefresh = outline.attr("autoRefresh").ifBlank { outline.attr("autorefresh") }
                    .toBooleanStrictOrNull() ?: true,
                refreshInterval = outline.attr("refreshInterval")
                    .ifBlank { outline.attr("refreshinterval") }
                    .toLongOrNull()?.coerceAtLeast(MIN_REFRESH_INTERVAL) ?: DEFAULT_REFRESH_INTERVAL,
            )
        }

    private fun validate(source: FeedSource) {
        require(source.id.isNotBlank()) { "Subscription id must not be blank" }
        require(source.name.isNotBlank()) { "Subscription name must not be blank" }
        require(source.url.startsWith("http://") || source.url.startsWith("https://")) {
            "Subscription URL must use http:// or https://"
        }
        require(source.refreshInterval >= MIN_REFRESH_INTERVAL) {
            "Refresh interval must be at least one minute"
        }
    }

    private fun stableId(url: String) = "feed-${url.hashCode().toUInt().toString(16)}"

    private fun String.escapeXml() = buildString(length) {
        this@escapeXml.forEach { char ->
            append(
                when (char) {
                    '&' -> "&amp;"
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '"' -> "&quot;"
                    '\'' -> "&apos;"
                    else -> char
                },
            )
        }
    }

    private companion object {
        const val MIN_REFRESH_INTERVAL = 60_000L
        const val DEFAULT_REFRESH_INTERVAL = 3_600_000L
    }
}

@Serializable
private data class ReaderSubscriptionBundle(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val sources: List<FeedSubscriptionSnapshot>,
) {
    init {
        require(format == FORMAT) { "Unsupported subscription format: $format" }
        require(version == VERSION) { "Unsupported subscription version: $version" }
    }

    companion object {
        const val FORMAT = "thread-reader-subscriptions"
        const val VERSION = 1
    }
}

@Serializable
private data class FeedSubscriptionSnapshot(
    val id: String,
    val name: String,
    val url: String,
    val type: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val selectorConfig: Map<String, String> = emptyMap(),
    val autoRefresh: Boolean = true,
    val refreshInterval: Long = 3_600_000,
) {
    fun toDomain() = FeedSource(
        id = id,
        name = name,
        url = url,
        type = FeedType.valueOf(type),
        description = description,
        iconUrl = iconUrl,
        selectorConfig = selectorConfig,
        autoRefresh = autoRefresh,
        refreshInterval = refreshInterval,
    )

    companion object {
        fun fromDomain(source: FeedSource) = FeedSubscriptionSnapshot(
            id = source.id,
            name = source.name,
            url = source.url,
            type = source.type.name,
            description = source.description,
            iconUrl = source.iconUrl,
            selectorConfig = source.selectorConfig,
            autoRefresh = source.autoRefresh,
            refreshInterval = source.refreshInterval,
        )
    }
}
