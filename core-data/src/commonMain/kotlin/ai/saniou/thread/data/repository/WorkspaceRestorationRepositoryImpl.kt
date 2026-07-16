package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.workspace.RestorableContentKind
import ai.saniou.thread.domain.model.workspace.RestorableContentReference
import ai.saniou.thread.domain.repository.WorkspaceRestorationRepository
import kotlinx.coroutines.withContext

class WorkspaceRestorationRepositoryImpl(
    private val database: Database,
) : WorkspaceRestorationRepository {
    override suspend fun isAvailable(reference: RestorableContentReference): Boolean = withContext(ioDispatcher) {
        when (reference.kind) {
            RestorableContentKind.TOPIC -> database.topicQueries
                .getTopic(reference.sourceId.orEmpty(), reference.id)
                .executeAsOneOrNull() != null
            RestorableContentKind.ARTICLE -> database.articleQueries
                .getArticleById(reference.id)
                .executeAsOneOrNull() != null
        }
    }
}
