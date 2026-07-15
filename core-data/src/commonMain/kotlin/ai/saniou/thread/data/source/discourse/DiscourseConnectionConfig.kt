package ai.saniou.thread.data.source.discourse

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Connection details for one Discourse instance.
 *
 * The development API key is intentionally injectable so local testing can keep a reusable
 * credential while the login workflow remains available to replace it at runtime later.
 */
data class DiscourseConnectionConfig(
    val sourceId: String,
    val displayName: String,
    val baseUrl: String,
    val developmentApiKey: String? = null,
)

/** Runtime credential overrides the reusable development credential without rebuilding the client. */
class DiscourseCredentialProvider(
    private val sourceId: String,
    private val developmentApiKey: String?,
    accountRepository: ai.saniou.thread.domain.repository.AccountRepository,
) {
    var apiKey: String? = developmentApiKey
        private set

    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + ai.saniou.corecommon.coroutines.ioDispatcher
    )
    private val accountJob = scope.launch {
        accountRepository.getCurrentAccount(sourceId).collect { account ->
            apiKey = account?.value?.takeIf(String::isNotBlank) ?: developmentApiKey
        }
    }

    fun update(apiKey: String) {
        this.apiKey = apiKey
    }

    fun close() {
        accountJob.cancel()
        scope.cancel()
    }
}
