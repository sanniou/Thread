package ai.saniou.thread.domain.model.source

import kotlin.jvm.JvmInline

/** Open connector kind identifier; adding a data connector must not require changing domain enums. */
@JvmInline
value class SourceType(val value: String) {
    init {
        require(value.matches(Regex("[a-z][a-z0-9_-]{1,63}"))) {
            "Source type must contain 2-64 lowercase letters, digits, '_' or '-'"
        }
    }

    override fun toString(): String = value

    companion object {
        val NMB = SourceType("nmb")
        val TIEBA = SourceType("tieba")
        val DISCOURSE = SourceType("discourse")
    }
}

/** Persistable, UI-editable description of one runtime source instance. */
data class SourceDescriptor(
    val id: String,
    val type: SourceType,
    val displayName: String,
    val baseUrl: String? = null,
    val enabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val options: Map<String, String> = emptyMap(),
) {
    init {
        require(id.matches(Regex("[a-z0-9][a-z0-9_-]{1,63}"))) {
            "Source id must contain 2-64 lowercase letters, digits, '_' or '-'"
        }
        require(displayName.isNotBlank()) { "Source display name must not be blank" }
        if (type == SourceType.DISCOURSE) {
            require(!baseUrl.isNullOrBlank()) { "Discourse requires a base URL" }
            require(baseUrl.startsWith("https://") || baseUrl.startsWith("http://")) {
                "Discourse base URL must use http:// or https://"
            }
        }
    }
}
