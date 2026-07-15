package ai.saniou.thread.data.source.discourse

/**
 * Connection details for one Discourse instance.
 *
 * The development API key is intentionally injectable so local testing can keep a reusable
 * credential while the login workflow remains available to replace it at runtime later.
 */
data class DiscourseConnectionConfig(
    val sourceId: String,
    val baseUrl: String,
    val developmentApiKey: String? = null,
)
