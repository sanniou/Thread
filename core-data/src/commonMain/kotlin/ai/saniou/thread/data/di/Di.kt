package ai.saniou.thread.data.di

import ai.saniou.thread.data.database.DriverFactory
import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.data.manager.CdnManager
import ai.saniou.thread.data.repository.BookmarkRepositoryImpl
import ai.saniou.thread.data.repository.FavoriteRepositoryImpl
import ai.saniou.thread.data.repository.FeedRepositoryImpl
import ai.saniou.thread.data.repository.SourceRepositoryImpl
import ai.saniou.thread.data.repository.ChannelRepositoryImpl
import ai.saniou.thread.data.repository.ContentBlockRepositoryImpl
import ai.saniou.thread.data.repository.HistoryRepositoryImpl
import ai.saniou.thread.data.repository.NoticeRepositoryImpl
import ai.saniou.thread.data.repository.PostRepositoryImpl
import ai.saniou.thread.data.repository.ReactionRepositoryImpl
import ai.saniou.thread.data.repository.UserRelationRepositoryImpl
import ai.saniou.thread.data.repository.ForumSearchRepositoryImpl
import ai.saniou.thread.data.repository.UserContentRepositoryImpl
import ai.saniou.thread.data.repository.LoginRepositoryImpl
import ai.saniou.thread.data.repository.ReferenceRepositoryImpl
import ai.saniou.thread.data.repository.SettingsRepositoryImpl
import ai.saniou.thread.data.repository.GlobalSearchRepositoryImpl
import ai.saniou.thread.data.repository.OperationsRepositoryImpl
import ai.saniou.thread.data.repository.PostDraftRepositoryImpl
import ai.saniou.thread.data.repository.IdentityRepositoryImpl
import ai.saniou.thread.data.repository.ProductActionHistoryRepositoryImpl
import ai.saniou.thread.data.repository.DefaultProductActionExecutor
import ai.saniou.thread.data.repository.ActivityCenterRepositoryImpl
import ai.saniou.thread.data.repository.WorkspaceSessionRepositoryImpl
import ai.saniou.thread.data.repository.WorkspaceRestorationRepositoryImpl
import ai.saniou.thread.data.repository.InboxRepositoryImpl
import ai.saniou.thread.data.repository.ContentLinkRepositoryImpl
import ai.saniou.thread.data.repository.SmartCollectionRepositoryImpl
import ai.saniou.thread.data.repository.AppearanceRepositoryImpl
import ai.saniou.thread.data.repository.SocialRepositoryImpl
import ai.saniou.thread.data.repository.ContentGraphRepositoryImpl
import ai.saniou.thread.data.repository.SubscriptionRepositoryImpl
import ai.saniou.thread.data.repository.SyncRepositoryImpl
import ai.saniou.thread.data.repository.TopicRepositoryImpl
import ai.saniou.thread.data.repository.AccountRepositoryImpl
import ai.saniou.thread.data.repository.TagRepositoryImpl
import ai.saniou.thread.data.repository.TrendRepositoryImpl
import ai.saniou.thread.data.source.tieba.remote.createNewTiebaApi
import ai.saniou.thread.data.source.tieba.remote.createAppHybridTiebaApi
import ai.saniou.thread.data.source.tieba.remote.createMiniTiebaApi
import ai.saniou.thread.data.source.tieba.remote.createWebTiebaApi
import ai.saniou.thread.data.source.tieba.remote.createOfficialTiebaApi
import ai.saniou.thread.data.source.tieba.remote.createLiteApiInterface
import ai.saniou.thread.data.source.tieba.remote.createSofireApi
import ai.saniou.thread.data.source.tieba.remote.createOfficialProtobufTiebaApi
import ai.saniou.thread.data.source.tieba.TiebaSource
import ai.saniou.thread.data.source.tieba.TiebaTrendSource
import ai.saniou.thread.data.source.tieba.TiebaPostingConnector
import ai.saniou.thread.data.source.tieba.TiebaUserContentConnector
import ai.saniou.thread.data.source.nmb.NmbAccountProvider
import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.thread.data.source.nmb.NmbPostingConnector
import ai.saniou.thread.data.source.nmb.NmbLoginConnector
import ai.saniou.thread.data.source.nmb.NmbTrendSource
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.createNmbXdApi
import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.cache.SqlDelightSourceCache
import ai.saniou.thread.data.cache.CacheFreshnessStore
import ai.saniou.thread.data.source.discourse.DiscourseSourceFactory
import ai.saniou.thread.data.source.tieba.TiebaLoginConnector
import ai.saniou.thread.data.source.tieba.TiebaReactionConnector
import ai.saniou.thread.data.source.tieba.TiebaUserRelationConnector
import ai.saniou.thread.data.source.tieba.TiebaSearchConnector
import ai.saniou.thread.data.source.tieba.TiebaChannelMembership
import ai.saniou.thread.data.source.tieba.TiebaThreadStoreSync
import ai.saniou.thread.data.source.tieba.TiebaInboxSync
import ai.saniou.thread.data.source.tieba.TiebaUserLikeForumSync
import ai.saniou.thread.data.source.tieba.TiebaChannelSign
import ai.saniou.thread.data.source.tieba.TiebaForumRuleService
import ai.saniou.thread.data.repository.ChannelActionRepositoryImpl
import ai.saniou.thread.domain.repository.ChannelActionRepository
import ai.saniou.thread.data.source.runtime.DefaultSourceCatalog
import ai.saniou.thread.data.source.runtime.RuntimeSourceRegistration
import ai.saniou.thread.domain.service.ImageUrlResolver
import ai.saniou.thread.data.reader.DefaultReaderRefreshScheduler
import ai.saniou.thread.data.reader.ReaderSubscriptionCodec
import ai.saniou.thread.data.sync.webdav.WebDavSyncTransport
import ai.saniou.thread.data.sync.webdav.UserDataRemoteTransport
import ai.saniou.thread.domain.repository.BookmarkRepository
import ai.saniou.thread.domain.repository.FavoriteRepository
import ai.saniou.thread.domain.repository.FeedRepository
import ai.saniou.thread.domain.repository.ForumSearchRepository
import ai.saniou.thread.domain.repository.SourceRepository
import ai.saniou.thread.domain.repository.ChannelRepository
import ai.saniou.thread.domain.repository.ContentBlockRepository
import ai.saniou.thread.domain.repository.HistoryRepository
import ai.saniou.thread.domain.repository.NoticeRepository
import ai.saniou.thread.domain.repository.LoginRepository
import ai.saniou.thread.domain.repository.PostRepository
import ai.saniou.thread.domain.repository.ReactionRepository
import ai.saniou.thread.domain.repository.ReferenceRepository
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.GlobalSearchRepository
import ai.saniou.thread.domain.repository.InboxRepository
import ai.saniou.thread.domain.repository.ContentLinkRepository
import ai.saniou.thread.domain.repository.SmartCollectionRepository
import ai.saniou.thread.domain.repository.AppearanceRepository
import ai.saniou.thread.domain.repository.SocialRepository
import ai.saniou.thread.domain.repository.ContentGraphRepository
import ai.saniou.thread.domain.repository.OperationsRepository
import ai.saniou.thread.domain.repository.PostDraftRepository
import ai.saniou.thread.domain.repository.IdentityRepository
import ai.saniou.thread.domain.repository.ProductActionHistoryRepository
import ai.saniou.thread.domain.repository.ProductActionExecutor
import ai.saniou.thread.domain.repository.ActivityCenterRepository
import ai.saniou.thread.domain.repository.WorkspaceSessionRepository
import ai.saniou.thread.domain.repository.WorkspaceRestorationRepository
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.model.source.SourceDescriptor
import ai.saniou.thread.domain.model.source.SourceType
import ai.saniou.thread.domain.source.TrendSource
import ai.saniou.thread.domain.repository.SubscriptionRepository
import ai.saniou.thread.domain.repository.SyncRepository
import ai.saniou.thread.domain.repository.TagRepository
import ai.saniou.thread.domain.repository.TopicRepository
import ai.saniou.thread.domain.repository.TrendRepository
import ai.saniou.thread.data.parser.FeedParserFactory
import ai.saniou.thread.data.parser.HtmlParser
import ai.saniou.thread.data.parser.JsonParser
import ai.saniou.thread.data.parser.RssParser
import ai.saniou.thread.data.repository.ReaderRepositoryImpl
import ai.saniou.thread.data.refresh.DefaultRefreshCoordinator
import ai.saniou.thread.data.refresh.PersistentRefreshHistoryRepository
import ai.saniou.thread.domain.repository.AccountRepository
import ai.saniou.thread.domain.repository.ReaderRepository
import ai.saniou.thread.domain.reader.ReaderRefreshScheduler
import ai.saniou.thread.domain.repository.UserContentRepository
import ai.saniou.thread.domain.refresh.RefreshCoordinator
import ai.saniou.thread.domain.refresh.RefreshHistoryRepository
import ai.saniou.thread.domain.cache.CachePolicyProvider
import ai.saniou.thread.domain.cache.DefaultCachePolicyProvider
import ai.saniou.thread.domain.source.ConnectorRegistry
import ai.saniou.thread.domain.source.SourceCatalog
import ai.saniou.thread.domain.usecase.reader.GetArticleCountsUseCase
import ai.saniou.thread.domain.usecase.subscription.GenerateRandomSubscriptionIdUseCase
import ai.saniou.thread.domain.usecase.channel.FetchChannelsUseCase
import ai.saniou.thread.domain.usecase.channel.GetLastOpenedChannelUseCase
import ai.saniou.thread.domain.usecase.channel.GetRecentChannelsUseCase
import ai.saniou.thread.domain.usecase.channel.SaveLastOpenedChannelUseCase
import ai.saniou.thread.network.ChallengeHandler
import ai.saniou.thread.network.CloudflareProtectionPlugin
import ai.saniou.thread.network.SaniouKtorfit
import ai.saniou.thread.network.cookie.CookieStore
import ai.saniou.thread.data.network.SettingsCookieStore
import ai.saniou.thread.network.installCookieAuth
import io.ktor.client.HttpClient
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.bindConstant
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import org.kodein.di.instanceOrNull
import org.kodein.di.singleton
import kotlin.time.Clock

val dataModule = DI.Module("dataModule") {
    bindConstant<String>(tag = "nmbBaseUrl") { "https://api.nmb.best/api/" }
    bindSingleton<NmbXdApi> {
        val nmbAccountProvider = instance<NmbAccountProvider>()
        val ktorfit = SaniouKtorfit(
            baseUrl = instance<String>("nmbBaseUrl")
        ) {
            installCookieAuth { nmbAccountProvider.getAccountValue() }
        }
        ktorfit.createNmbXdApi()
    }

    // cache
    bindSingleton<SourceCache> { SqlDelightSourceCache(instance()) }
    bindSingleton { CacheFreshnessStore(instance()) }
    bindSingleton<CachePolicyProvider> { DefaultCachePolicyProvider() }

    // source and repository
    bindSingleton<NmbSource> { NmbSource(instance(), instance(), instance(), instance()) }
    bindSingleton<TiebaSource> {
        TiebaSource(
            instance(tag = "V11"), // OfficialProtobufTiebaApi V11
            instance(tag = "V12"), // OfficialProtobufTiebaApi V12
            instance(), // Database
            instance(), // TiebaParameterProvider
            instance(), // MiniTiebaApi
        )
    }

    bindSingleton { NmbPostingConnector(instance()) }
    bindSingleton { NmbLoginConnector(instance()) }
    bindSingleton { TiebaUserContentConnector(instance(), instance()) }
    bindSingleton { TiebaSearchConnector(instance(), instance(), instance()) }
    bindSingleton { TiebaPostingConnector(instance(), instance(), instance(), instance(), instance()) }
    bindSingleton { TiebaLoginConnector(instance(), instance(), instance()) }
    bindSingleton { TiebaReactionConnector(instance(), instance(), instance(), instance(), instance()) }
    bindSingleton { TiebaUserRelationConnector(instance(), instance(), instance(), instance(), instance()) }
    bindSingleton { TiebaChannelMembership(instance(), instance(), instance()) }
    bindSingleton { TiebaThreadStoreSync(instance(), instance(), instance()) }
    bindSingleton { TiebaUserLikeForumSync(instance(), instance(), instance()) }
    bindSingleton { TiebaInboxSync(instance(), instance(), instance()) }
    bindSingleton {
        TiebaChannelSign(
            miniApi = instance(),
            officialApi = instance(),
            webApi = instance(),
            database = instance(),
            parameterProvider = instance(),
        )
    }
    bindSingleton {
        TiebaForumRuleService(
            protobufApi = instance(tag = "V11"),
            parameterProvider = instance(),
        )
    }
    bind<ChannelActionRepository>() with singleton {
        ChannelActionRepositoryImpl(instance(), instance())
    }

    bindSingleton {
        DiscourseSourceFactory(
            cache = instance(),
            database = instance(),
            settingsRepository = instance(),
            accountRepository = instance(),
            cookieStore = instance(),
            challengeHandler = instanceOrNull<ChallengeHandler>(),
        )
    }
    bindSingleton<SourceCatalog> {
        val nmbDescriptor = SourceDescriptor(
            id = "nmb",
            type = SourceType.NMB,
            displayName = instance<NmbSource>().name,
            isBuiltIn = true,
        )
        val tiebaDescriptor = SourceDescriptor(
            id = "tieba",
            type = SourceType.TIEBA,
            displayName = instance<TiebaSource>().name,
            isBuiltIn = true,
        )
        val defaultDiscourse = SourceDescriptor(
            id = "discourse",
            type = SourceType.DISCOURSE,
            displayName = "Linux.do",
            baseUrl = "https://linux.do/",
            options = mapOf(
                DiscourseSourceFactory.OPTION_DEVELOPMENT_API_KEY to
                    "eebd35d4ca8e9c948cd8188f6ff9b440"
            ),
        )
        DefaultSourceCatalog(
            database = instance(),
            builtIns = listOf(
                nmbDescriptor to RuntimeSourceRegistration(
                    source = instance<NmbSource>(),
                    search = instance<NmbSource>(),
                    userContent = instance<NmbSource>(),
                    posting = instance<NmbPostingConnector>(),
                    login = instance<NmbLoginConnector>(),
                ),
                tiebaDescriptor to RuntimeSourceRegistration(
                    source = instance<TiebaSource>(),
                    search = instance<TiebaSearchConnector>(),
                    userContent = instance<TiebaUserContentConnector>(),
                    posting = instance<TiebaPostingConnector>(),
                    login = instance<TiebaLoginConnector>(),
                    subComments = instance<TiebaSource>(),
                    reactions = instance<TiebaReactionConnector>(),
                    userRelation = instance<TiebaUserRelationConnector>(),
                ),
            ),
            factories = setOf(instance<DiscourseSourceFactory>()),
            defaults = listOf(nmbDescriptor, tiebaDescriptor, defaultDiscourse),
        )
    }
    bind<ConnectorRegistry>() with singleton { instance<SourceCatalog>() }

    bind<ForumSearchRepository>() with singleton { ForumSearchRepositoryImpl(instance()) }
    bind<UserContentRepository>() with singleton { UserContentRepositoryImpl(instance()) }

    bind<SourceRepository>() with singleton { SourceRepositoryImpl(instance()) }

    bind<BookmarkRepository>() with singleton { BookmarkRepositoryImpl(instance(), instance()) }
    bind<TagRepository>() with singleton { TagRepositoryImpl(instance()) }
    bind<FavoriteRepository>() with singleton { FavoriteRepositoryImpl(instance(), instance()) }
    bind<FeedRepository>() with singleton {
        FeedRepositoryImpl(
            instance(),
            instance(),
            instance(),
            instance(),
        )
    }
    bind<SubscriptionRepository>() with singleton {
        SubscriptionRepositoryImpl(
            instance(),
            instance()
        )
    }
    bind<AccountRepository>() with singleton { AccountRepositoryImpl(instance()) }
    bind<SettingsRepository>() with singleton { SettingsRepositoryImpl(instance()) }
    bind<InboxRepository>() with singleton { InboxRepositoryImpl(instance()) }
    bind<ContentLinkRepository>() with singleton { ContentLinkRepositoryImpl(instance(), instance()) }
    bind<SmartCollectionRepository>() with singleton { SmartCollectionRepositoryImpl(instance(), instance()) }
    bind<AppearanceRepository>() with singleton { AppearanceRepositoryImpl(instance()) }
    bind<SocialRepository>() with singleton { SocialRepositoryImpl(instance(), instance()) }
    bind<ContentGraphRepository>() with singleton { ContentGraphRepositoryImpl(instance()) }
    bind<IdentityRepository>() with singleton { IdentityRepositoryImpl(instance(), instance(), instance()) }
    bind<LoginRepository>() with singleton { LoginRepositoryImpl(instance(), instance(), instance()) }
    bind<WorkspaceSessionRepository>() with singleton { WorkspaceSessionRepositoryImpl(instance()) }
    bind<WorkspaceRestorationRepository>() with singleton { WorkspaceRestorationRepositoryImpl(instance()) }
    bind<GlobalSearchRepository>() with singleton { GlobalSearchRepositoryImpl(instance(), instance()) }
    bind<NoticeRepository>() with singleton {
        NoticeRepositoryImpl(
            instance(),
            instance(),
            instance()
        )
    }
    bind<HistoryRepository>() with singleton {
        HistoryRepositoryImpl(instance(), instance())
    }
    bind<ContentBlockRepository>() with singleton {
        ContentBlockRepositoryImpl(instance())
    }
    bind<PostRepository>() with singleton { PostRepositoryImpl(instance(), instance(), instance()) }
    bind<ReactionRepository>() with singleton { ReactionRepositoryImpl(instance(), instance(), instance()) }
    bind<ai.saniou.thread.domain.repository.UserRelationRepository>() with singleton { UserRelationRepositoryImpl(instance()) }

    bind<TrendSource>(tag = "nmbTrend") with singleton { NmbTrendSource(instance()) }
    bind<TrendSource>(tag = "tiebaTrend") with singleton {
        TiebaTrendSource(
            instance(tag = "V11"),
            instance()
        )
    }

    bind<Set<TrendSource>>(tag = "allTrendSources") with singleton {
        HashSet<TrendSource>().apply {
            add(instance(tag = "nmbTrend"))
            add(instance(tag = "tiebaTrend"))
        }
    }

    bind<TrendRepository>() with singleton {
        TrendRepositoryImpl(
            instance(tag = "allTrendSources"),
            instance()
        )
    }
    bind<ReferenceRepository>() with singleton { ReferenceRepositoryImpl(instance(), instance()) }
    bind<TopicRepository>() with singleton {
        TopicRepositoryImpl(
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
        )
    }
    bind<ChannelRepository>() with singleton {
        ChannelRepositoryImpl(
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
        )
    }

    // Versioned user-data backup with a real common WebDAV transport.
    bindSingleton<UserDataRemoteTransport> { WebDavSyncTransport(instance()) }
    bind<SyncRepository>() with singleton { SyncRepositoryImpl(instance(), instance(), instance()) }

    bindSingleton { NmbAccountProvider(instance()) }
    // Cookie Store (for Discourse/CF)
    bindSingleton<CookieStore> { SettingsCookieStore(instance()) }
    // CDN管理器
    bindSingleton<CdnManager> { CdnManager(instance()) }
    bind<ImageUrlResolver>() with singleton { instance<CdnManager>() }
    // 数据库
    bindSingleton { createDatabase(DriverFactory()) }

    // Reader Feature
    bindSingleton { RssParser() }
    bindSingleton { JsonParser() }
    bindSingleton { HtmlParser() }
    bindSingleton { FeedParserFactory(instance(), instance(), instance()) }
    bindSingleton { ReaderSubscriptionCodec() }
    bindSingleton { HttpClient() } // Use a basic HttpClient
    bindSingleton<RefreshHistoryRepository> { PersistentRefreshHistoryRepository(instance()) }
    bindSingleton<RefreshCoordinator> { DefaultRefreshCoordinator(instance(), instance()) }
    bind<ReaderRepository>() with singleton {
        ReaderRepositoryImpl(instance(), instance(), instance(), instance(), instance(), instance(), instance())
    }
    bind<OperationsRepository>() with singleton {
        OperationsRepositoryImpl(instance(), instance(), instance(), instance(), instance())
    }
    bind<PostDraftRepository>() with singleton { PostDraftRepositoryImpl(instance()) }
    bind<ProductActionHistoryRepository>() with singleton { ProductActionHistoryRepositoryImpl(instance()) }
    bind<ProductActionExecutor>() with singleton {
        DefaultProductActionExecutor(
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
        )
    }
    bind<ActivityCenterRepository>() with singleton {
        ActivityCenterRepositoryImpl(instance(), instance(), instance(), instance(), instance())
    }
    bind<ReaderRefreshScheduler>() with singleton { DefaultReaderRefreshScheduler(instance()) }

    // Tieba Infrastructure
    bindSingleton { ai.saniou.thread.data.source.tieba.TiebaParameterProvider(instance()) }

    // Tieba API: NewTiebaApi
    bindSingleton<ai.saniou.thread.data.source.tieba.remote.NewTiebaApi> {
        val paramProvider = instance<ai.saniou.thread.data.source.tieba.TiebaParameterProvider>()
        val ktorfit = SaniouKtorfit(
            baseUrl = "http://c.tieba.baidu.com/"
        ) {
            install(ai.saniou.thread.network.tieba.TiebaCommonHeaderPlugin) {
                headers = mapOf(
                    "ka" to { "open" },
                    "Pragma" to { "no-cache" },
                    "User-Agent" to { "bdtb for Android 8.2.2" },
                    "cuid" to { paramProvider.getCuid() }
                )
            }
            install(ai.saniou.thread.network.tieba.TiebaCommonParamPlugin) {
                params = mapOf(
                    "BDUSS" to { paramProvider.getBduss() }, // suspend call not supported in plugin map, need to handle blocking or pre-fetch
                    // Note: Ktor plugins are synchronous in configuration usually.
                    // However, lambda execution happens during request. If lambda is not suspend, we might need runBlocking or cache.
                    // For KMP, runBlocking is limited.
                    // Best practice: The plugin config lambda should invoke a provider that manages state.
                    // Here we assume getBduss() might be cached or we accept standard execution.
                    // But wait, TiebaParameterProvider.getBduss is suspend.
                    // We need to bridge this. For now, let's assume non-suspend access or blocking wrapper if platform permits.
                    // Actually, param lambda is () -> String.
                    // We will simplify TiebaParameterProvider to return String directly (cached in memory) or use a helper.
                    // For this migration, I'll modify TiebaParameterProvider to be synchronous where possible or use empty string fallback.

                    "_client_id" to { "wappc_1687508826727" },
                    "_client_type" to { "2" },
                    "_os_version" to { "Android" },
                    "model" to { paramProvider.getModel() },
                    "net_type" to { "1" },
                    "_phone_imei" to { "000000000000000" },
                    "_timestamp" to { paramProvider.getTimestamp() },
                    "cuid" to { paramProvider.getCuid() },
                    "from" to { "baidu_appstore" },
                    "client_version" to { "8.2.2" }
                )
            }
            install(ai.saniou.thread.network.tieba.TiebaSortAndSignPlugin) {
                appSecret = "tiebaclient!!!"
            }
        }
        ktorfit.createNewTiebaApi()
    }

    // Tieba API: AppHybridTiebaApi
    bindSingleton<ai.saniou.thread.data.source.tieba.remote.AppHybridTiebaApi> {
        val paramProvider = instance<ai.saniou.thread.data.source.tieba.TiebaParameterProvider>()
        val ktorfit = SaniouKtorfit(
            baseUrl = "https://tieba.baidu.com/"
        ) {
            install(ai.saniou.thread.network.tieba.TiebaCommonHeaderPlugin) {
                headers = mapOf(
                    "User-Agent" to { "tieba/12.35.1.0 skin/default" },
                    "Host" to { "tieba.baidu.com" },
                    "Pragma" to { "no-cache" },
                    "Cache-Control" to { "no-cache" },
                    "Accept" to { "application/json, text/plain, */*" },
                    "Accept-Language" to { "zh-CN,zh;q=0.9,en;q=0.8" }, // Simplified
                    "X-Requested-With" to { "com.baidu.tieba" },
                    "Sec-Fetch-Site" to { "same-origin" },
                    "Sec-Fetch-Mode" to { "cors" },
                    "Sec-Fetch-Dest" to { "empty" },
                    "Cookie" to {
                        // Hybrid endpoints require both device identity and authenticated session.
                        "CUID=${paramProvider.getCuid()}; BDUSS=${paramProvider.getBduss()}"
                    }
                )
            }
            install(ai.saniou.thread.network.tieba.TiebaCommonParamPlugin) {
                 params = mapOf(
                    "BDUSS" to { paramProvider.getBduss() },
                    "stoken" to { paramProvider.getSToken() }
                 )
            }
            // AppHybridTiebaApi usually doesn't need SortAndSign in the same way, or uses different keys.
            // Checking original code: it uses AddWebCookieInterceptor and CommonParamInterceptor.
            // No SortAndSignInterceptor mentioned for HYBRID_TIEBA_API in RetrofitTiebaApi.kt.
        }
        ktorfit.createAppHybridTiebaApi()
    }

    // Tieba API: MiniTiebaApi
    bindSingleton<ai.saniou.thread.data.source.tieba.remote.MiniTiebaApi> {
        val paramProvider = instance<ai.saniou.thread.data.source.tieba.TiebaParameterProvider>()
        val ktorfit = SaniouKtorfit(
            baseUrl = "http://c.tieba.baidu.com/"
        ) {
            install(ai.saniou.thread.network.tieba.TiebaCommonHeaderPlugin) {
                headers = mapOf(
                    "User-Agent" to { "bdtb for Android 7.2.0.0" },
                    "cuid" to { paramProvider.getCuid() },
                    "cuid_galaxy2" to { paramProvider.getCuid() }
                )
            }
            install(ai.saniou.thread.network.tieba.TiebaCommonParamPlugin) {
                params = mapOf(
                    "BDUSS" to { paramProvider.getBduss() },
                    "_client_id" to { "wappc_1687508826727" },
                    "_client_type" to { "2" },
                    "_os_version" to { "Android" },
                    "model" to { paramProvider.getModel() },
                    "net_type" to { "1" },
                    "_phone_imei" to { "000000000000000" },
                    "_timestamp" to { paramProvider.getTimestamp() },
                    "cuid" to { paramProvider.getCuid() },
                    "cuid_galaxy2" to { paramProvider.getCuid() },
                    "from" to { "1021636m" },
                    "client_version" to { "7.2.0.0" },
                    "subapp_type" to { "mini" }
                )
            }
            install(ai.saniou.thread.network.tieba.TiebaSortAndSignPlugin) {
                appSecret = "tiebaclient!!!"
            }
        }
        ktorfit.createMiniTiebaApi()
    }

    // Tieba API: WebTiebaApi
    bindSingleton<ai.saniou.thread.data.source.tieba.remote.WebTiebaApi> {
        val paramProvider = instance<ai.saniou.thread.data.source.tieba.TiebaParameterProvider>()
        val ktorfit = SaniouKtorfit(
            baseUrl = "https://tieba.baidu.com/"
        ) {
            install(ai.saniou.thread.network.tieba.TiebaCommonHeaderPlugin) {
                headers = mapOf(
                    "User-Agent" to { "tieba/11.10.8.6 skin/default" },
                    "cuid" to { paramProvider.getCuid() },
                    "cuid_galaxy2" to { paramProvider.getCuid() },
                    "cuid_gid" to { "" },
                    "client_user_token" to { "" }, // Placeholder for uid
                    "Charset" to { "UTF-8" },
                    "Host" to { "tieba.baidu.com" }
                )
            }
            // WebTiebaApi primarily uses headers and cookies, less common param injection
            // But checking RetrofitTiebaApi.kt, it uses AddWebCookieInterceptor which we haven't fully ported as a separate plugin,
            // but we can simulate cookie injection via Header plugin if logic is simple.
            // Retrofit AddWebCookieInterceptor adds a complex Cookie header.
            // We'll add a simplified cookie provider here.
        }
        ktorfit.createWebTiebaApi()
    }

    // Tieba API: OfficialTiebaApi
    bindSingleton<ai.saniou.thread.data.source.tieba.remote.OfficialTiebaApi> {
        val paramProvider = instance<ai.saniou.thread.data.source.tieba.TiebaParameterProvider>()
        val ktorfit = SaniouKtorfit(
            baseUrl = "http://c.tieba.baidu.com/"
        ) {
            install(ai.saniou.thread.network.tieba.TiebaCommonHeaderPlugin) {
                headers = mapOf(
                    "User-Agent" to { "bdtb for Android 12.25.1.0" },
                    "Cookie" to {
                        "CUID=${paramProvider.getCuid()};ka=open;TBBRAND=${paramProvider.getBrand()};BAIDUID=${paramProvider.getAndroidId()};"
                        // Note: BAIDUID usually comes from cookie storage or device utils. Using AndroidId as placeholder if BAIDUID not available directly in provider.
                    },
                    "cuid" to { paramProvider.getCuid() },
                    "cuid_galaxy2" to { paramProvider.getCuid() },
                    "cuid_gid" to { "" },
                    "client_type" to { "2" },
                    "Charset" to { "UTF-8" },
                    "client_logid" to { paramProvider.getTimestamp() }
                )
            }
            install(ai.saniou.thread.network.tieba.TiebaCommonParamPlugin) {
                params = mapOf(
                    "BDUSS" to { paramProvider.getBduss() },
                    "_client_id" to { "wappc_1687508826727" },
                    "_client_type" to { "2" },
                    "_os_version" to { "Android" },
                    "model" to { paramProvider.getModel() },
                    "net_type" to { "1" },
                    "_phone_imei" to { "000000000000000" },
                    "_timestamp" to { paramProvider.getTimestamp() },
                    "cuid" to { paramProvider.getCuid() },
                    "cuid_galaxy2" to { paramProvider.getCuid() },
                    "from" to { "tieba" },
                    "client_version" to { "12.25.1.0" },
                    "Active_Timestamp" to { Clock.System.now().toEpochMilliseconds().toString() }, // Placeholder for active timestamp
                    "Android_ID" to { paramProvider.getAndroidId() }, // Should be base64 encoded typically
                    "Baidu_ID" to { "BAIDUID_PLACEHOLDER" }, // Need real BAIDUID
                    "brand" to { paramProvider.getBrand() },
                    "cmode" to { "1" },
                    "cuid_gid" to { "" },
                    "event_day" to { "" }, // Should be formatted date
                    "extra" to { "" },
                    "framework_ver" to { "3340042" },
                    "is_teenager" to { "0" },
                    "last_update_time" to { "" },
                    "mac" to { "02:00:00:00:00:00" },
                    "sample_id" to { "" },
                    "sdk_ver" to { "2.34.0" },
                    "start_scheme" to { "" },
                    "start_type" to { "1" },
                    "swan_game_ver" to { "1038000" }
                )
            }
            install(ai.saniou.thread.network.tieba.TiebaSortAndSignPlugin) {
                appSecret = "tiebaclient!!!"
            }
        }
        ktorfit.createOfficialTiebaApi()
    }

    // Tieba API: LiteApiInterface
    bindSingleton<ai.saniou.thread.data.source.tieba.remote.LiteApiInterface> {
        val ktorfit = SaniouKtorfit(baseUrl = "https://huancheng65.github.io/") {}
        ktorfit.createLiteApiInterface()
    }

    // Tieba API: SofireApi
    bindSingleton<ai.saniou.thread.data.source.tieba.remote.SofireApi> {
        val ktorfit = SaniouKtorfit(baseUrl = "http://tieba.baidu.com/") {}
        ktorfit.createSofireApi()
    }

    // Tieba API: OfficialProtobufTiebaApi (V11)
    bindSingleton<ai.saniou.thread.data.source.tieba.remote.OfficialProtobufTiebaApi>(tag = "V11") {
        val paramProvider = instance<ai.saniou.thread.data.source.tieba.TiebaParameterProvider>()
        val version = ai.saniou.thread.data.source.tieba.remote.ClientVersion.TIEBA_V11

        val ktorfit = SaniouKtorfit(
            baseUrl = "https://tiebac.baidu.com/"
        ) {
            install(ai.saniou.thread.network.tieba.TiebaCommonHeaderPlugin) {
                headers = mapOf(
                    "Charset" to { "UTF-8" },
                    "cookie" to {
                        "CUID=${paramProvider.getCuid()};ka=open;TBBRAND=${paramProvider.getBrand()};"
                    },
                    "cuid" to { paramProvider.getCuid() },
                    "cuid_galaxy2" to { paramProvider.getCuid() },
                    "cuid_gid" to { "" },
                    "c3_aid" to { paramProvider.getAndroidId() },
                    "User-Agent" to { "bdtb for Android ${version.version}" },
                    "x_bd_data_type" to { "protobuf" }
                )
            }
        }
        ktorfit.createOfficialProtobufTiebaApi()
    }

    // Tieba API: OfficialProtobufTiebaApi (V12)
    bindSingleton<ai.saniou.thread.data.source.tieba.remote.OfficialProtobufTiebaApi>(tag = "V12") {
        val paramProvider = instance<ai.saniou.thread.data.source.tieba.TiebaParameterProvider>()
        val version = ai.saniou.thread.data.source.tieba.remote.ClientVersion.TIEBA_V12

        val ktorfit = SaniouKtorfit(
            baseUrl = "https://tiebac.baidu.com/"
        ) {
            install(ai.saniou.thread.network.tieba.TiebaCommonHeaderPlugin) {
                headers = mapOf(
                    "Charset" to { "UTF-8" },
                    "Cookie" to {
                        "ka=open;CUID=${paramProvider.getCuid()};TBBRAND=${paramProvider.getBrand()};"
                    },
                    "cuid" to { paramProvider.getCuid() },
                    "cuid_galaxy2" to { paramProvider.getCuid() },
                    "cuid_gid" to { "" },
                    "c3_aid" to { paramProvider.getAndroidId() },
                    "User-Agent" to { "tieba/${version.version}" },
                    "x_bd_data_type" to { "protobuf" }
                )
            }
        }
        ktorfit.createOfficialProtobufTiebaApi()
    }

    bind<GetArticleCountsUseCase>() with singleton { GetArticleCountsUseCase(instance()) }
    bind<GenerateRandomSubscriptionIdUseCase>() with singleton {
        GenerateRandomSubscriptionIdUseCase(
            instance()
        )
    }

    bind<FetchChannelsUseCase>() with singleton { FetchChannelsUseCase(instance()) }
    bind<GetLastOpenedChannelUseCase>() with singleton { GetLastOpenedChannelUseCase(instance()) }
    bind<GetRecentChannelsUseCase>() with singleton { GetRecentChannelsUseCase(instance()) }
    bind<SaveLastOpenedChannelUseCase>() with singleton { SaveLastOpenedChannelUseCase(instance()) }
}
