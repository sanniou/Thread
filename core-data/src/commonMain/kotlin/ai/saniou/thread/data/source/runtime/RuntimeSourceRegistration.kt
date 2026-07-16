package ai.saniou.thread.data.source.runtime

import ai.saniou.thread.domain.model.source.SourceDescriptor
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.source.ForumSearchConnector
import ai.saniou.thread.domain.source.LoginConnector
import ai.saniou.thread.domain.source.PostingConnector
import ai.saniou.thread.domain.source.ReactionConnector
import ai.saniou.thread.domain.source.SubCommentConnector
import ai.saniou.thread.domain.source.UserContentConnector
import ai.saniou.thread.domain.source.SourceConformance

data class RuntimeSourceRegistration(
    val source: Source,
    val search: ForumSearchConnector? = null,
    val userContent: UserContentConnector? = null,
    val posting: PostingConnector? = null,
    val login: LoginConnector? = null,
    val subComments: SubCommentConnector? = null,
    val reactions: ReactionConnector? = null,
    val dispose: () -> Unit = {},
) {
    init {
        listOfNotNull(search, userContent, posting, login, subComments, reactions).forEach { connector ->
            require(connector.sourceId == source.id) {
                "Connector '${connector::class.simpleName}' does not match source '${source.id}'"
            }
        }
        SourceConformance.inspect(
            source = source,
            search = search,
            userContent = userContent,
            posting = posting,
            login = login,
            subComments = subComments,
            reactions = reactions,
        ).requireValid()
    }
}

interface RuntimeSourceFactory {
    val type: ai.saniou.thread.domain.model.source.SourceType
    fun create(descriptor: SourceDescriptor): RuntimeSourceRegistration
}
