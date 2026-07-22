package ai.saniou.thread.domain.source

import ai.saniou.thread.domain.repository.Source

/**
 * Runtime contract shared by built-in and dynamically created sources.
 *
 * A capability is a product promise. It may only be advertised when the matching connector is
 * registered, and registered connectors must be reachable through a capability. This turns UI
 * degradation from convention into a hard composition-root gate.
 */
data class SourceConformanceReport(
    val sourceId: String,
    val violations: List<String>,
) {
    val isValid: Boolean get() = violations.isEmpty()

    fun requireValid() {
        require(isValid) {
            "Source '$sourceId' violates its runtime contract: ${violations.joinToString("; ")}"
        }
    }
}

object SourceConformance {
    fun inspect(
        source: Source,
        search: ForumSearchConnector? = null,
        userContent: UserContentConnector? = null,
        posting: PostingConnector? = null,
        login: LoginConnector? = null,
        subComments: SubCommentConnector? = null,
        reactions: ReactionConnector? = null,
        userRelation: UserRelationConnector? = null,
    ): SourceConformanceReport {
        val capabilities = source.capabilities
        val violations = buildList {
            checkExact("search", capabilities.supportsSearch, search != null)
            checkExact("user content", capabilities.supportsUserContent, userContent != null)
            checkExact("login", capabilities.supportsLogin, login != null)
            checkExact("sub-comments", capabilities.hasSubComments, subComments != null)

            val promisesPosting = capabilities.supportsTopicCreation || capabilities.supportsReplies ||
                capabilities.supportsAttachments
            checkExact("posting", promisesPosting, posting != null)

            val promisesReactions = capabilities.hasUpvote || capabilities.hasDownvote
            checkExact("reactions", promisesReactions, reactions != null)
            checkExact("user relation", capabilities.supportsUserFollow, userRelation != null)

            if (capabilities.commentPageSize != null && capabilities.commentPageSize <= 0) {
                add("commentPageSize must be positive")
            }
            if (!capabilities.supportsPagination && capabilities.commentPageSize != null) {
                add("commentPageSize requires pagination")
            }
            if (capabilities.supportsAttachments && !capabilities.supportsReplies &&
                !capabilities.supportsTopicCreation
            ) {
                add("attachments require topic creation or replies")
            }
        }
        return SourceConformanceReport(source.id, violations)
    }

    private fun MutableList<String>.checkExact(
        capability: String,
        advertised: Boolean,
        registered: Boolean,
    ) {
        when {
            advertised && !registered -> add("$capability is advertised without a connector")
            !advertised && registered -> add("$capability connector is registered but not advertised")
        }
    }
}
