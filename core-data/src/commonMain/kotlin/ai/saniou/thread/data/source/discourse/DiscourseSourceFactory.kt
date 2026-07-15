package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.source.discourse.remote.createDiscourseApi
import ai.saniou.thread.data.source.runtime.RuntimeSourceFactory
import ai.saniou.thread.data.source.runtime.RuntimeSourceRegistration
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.source.SourceDescriptor
import ai.saniou.thread.domain.model.source.SourceType
import ai.saniou.thread.domain.repository.AccountRepository
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.network.ChallengeHandler
import ai.saniou.thread.network.CloudflareProtectionPlugin
import ai.saniou.thread.network.SaniouKtorfit
import ai.saniou.thread.network.cookie.CookieStore
import ai.saniou.thread.network.installCookieAuth
import ai.saniou.thread.network.installDynamicHeader

class DiscourseSourceFactory(
    private val cache: SourceCache,
    private val database: Database,
    private val settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
    private val cookieStore: CookieStore,
    private val challengeHandler: ChallengeHandler?,
) : RuntimeSourceFactory {
    override val type: SourceType = SourceType.DISCOURSE

    override fun create(descriptor: SourceDescriptor): RuntimeSourceRegistration {
        require(descriptor.type == type)
        val connection = DiscourseConnectionConfig(
            sourceId = descriptor.id,
            displayName = descriptor.displayName,
            baseUrl = descriptor.baseUrl.orEmpty().ensureTrailingSlash(),
            developmentApiKey = descriptor.options[OPTION_DEVELOPMENT_API_KEY]?.takeIf(String::isNotBlank),
        )
        val credentials = DiscourseCredentialProvider(
            sourceId = descriptor.id,
            developmentApiKey = connection.developmentApiKey,
            accountRepository = accountRepository,
        )
        val ktorfit = SaniouKtorfit(baseUrl = connection.baseUrl) {
            installCookieAuth { cookieStore.getCookie(connection.sourceId) }
            installDynamicHeader("User-Api-Key") { credentials.apiKey }
            challengeHandler?.let { handler ->
                install(CloudflareProtectionPlugin) {
                    this.challengeHandler = handler
                    sourceId = connection.sourceId
                }
            }
        }
        val api = ktorfit.createDiscourseApi()
        val source = DiscourseSource(api, cache, database, settingsRepository, connection)
        return RuntimeSourceRegistration(
            source = source,
            search = source,
            userContent = source,
            posting = DiscoursePostingConnector(source, api),
            login = DiscourseLoginConnector(source, credentials),
            dispose = {
                credentials.close()
                ktorfit.httpClient.close()
            },
        )
    }

    companion object {
        const val OPTION_DEVELOPMENT_API_KEY = "developmentApiKey"
    }
}

private fun String.ensureTrailingSlash(): String = trim().trimEnd('/') + "/"
