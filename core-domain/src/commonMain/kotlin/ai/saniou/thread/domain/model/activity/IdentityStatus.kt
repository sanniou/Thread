package ai.saniou.thread.domain.model.activity

enum class IdentityValidity {
    NOT_APPLICABLE,
    ANONYMOUS,
    VALID,
    EXPIRED,
    DISABLED,
}

enum class IdentityLoginKind {
    NONE,
    MANUAL,
    WEB,
    API,
}

/** Credential-free projection of one source identity. */
data class SourceIdentityStatus(
    val sourceId: String,
    val sourceName: String,
    val validity: IdentityValidity,
    val loginKind: IdentityLoginKind,
    val supportsLogin: Boolean,
    val accountId: String? = null,
    val accountAlias: String? = null,
    val accountAvatar: String? = null,
    val lastValidatedAtEpochMillis: Long? = null,
    val message: String? = null,
) {
    val requiresAuthentication: Boolean get() = validity == IdentityValidity.EXPIRED
    val isActionable: Boolean get() = supportsLogin && validity in setOf(
        IdentityValidity.ANONYMOUS,
        IdentityValidity.EXPIRED,
    )
}
