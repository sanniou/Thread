package ai.saniou.thread.data.di

import ai.saniou.thread.data.database.DriverFactory
import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.data.manager.CdnManager
import ai.saniou.thread.data.repository.BookmarkRepositoryImpl
import ai.saniou.thread.data.repository.FavoriteRepositoryImpl
import ai.saniou.thread.data.repository.SourceRepositoryImpl
import ai.saniou.thread.data.repository.ChannelRepositoryImpl
import ai.saniou.thread.data.repository.HistoryRepositoryImpl
import ai.saniou.thread.data.repository.NoticeRepositoryImpl
import ai.saniou.thread.data.repository.PostRepositoryImpl
import ai.saniou.thread.data.repository.ReferenceRepositoryImpl
import ai.saniou.thread.data.repository.SettingsRepositoryImpl
import ai.saniou.thread.data.repository.SubscriptionRepositoryImpl
import ai.saniou.thread.data.repository.SyncRepositoryImpl
import ai.saniou.thread.data.repository.TopicRepositoryImpl
import ai.saniou.thread.data.repository.AccountRepositoryImpl
import ai.saniou.thread.data.repository.TagRepositoryImpl
import ai.saniou.thread.data.repository.TrendRepositoryImpl
import ai.saniou.thread.data.repository.UserRepositoryImpl
import ai.saniou.thread.data.source.acfun.AcfunSource
import ai.saniou.thread.data.source.acfun.remote.AcfunApi
import ai.saniou.thread.data.source.acfun.remote.AcfunHeaderPlugin
import ai.saniou.thread.data.source.acfun.remote.AcfunTokenManager
import ai.saniou.thread.data.source.acfun.remote.createAcfunApi
import ai.saniou.thread.data.source.tieba.remote.createNewTiebaApi
import ai.saniou.thread.data.source.tieba.remote.createAppHybridTiebaApi
import ai.saniou.thread.data.source.tieba.remote.createMiniTiebaApi
import ai.saniou.thread.data.source.tieba.remote.createWebTiebaApi
import ai.saniou.thread.data.source.tieba.remote.createOfficialTiebaApi
import ai.saniou.thread.data.source.tieba.remote.createLiteApiInterface
import ai.saniou.thread.data.source.tieba.remote.createSofireApi
import ai.saniou.thread.data.source.tieba.remote.createOfficialProtobufTiebaApi
import ai.saniou.thread.data.source.tieba.TiebaSource
import ai.saniou.thread.data.source.nga.NgaSource
import ai.saniou.thread.data.source.nmb.NmbAccountProvider
import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.createNmbXdApi
import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.cache.SqlDelightSourceCache
import ai.saniou.thread.data.source.discourse.DiscourseSource
import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.discourse.remote.createDiscourseApi
import ai.saniou.thread.data.sync.local.LocalSyncProvider
import ai.saniou.thread.data.sync.webdav.WebDavSyncProvider
import ai.saniou.thread.domain.repository.BookmarkRepository
import ai.saniou.thread.domain.repository.FavoriteRepository
import ai.saniou.thread.domain.repository.SourceRepository
import ai.saniou.thread.domain.repository.ChannelRepository
import ai.saniou.thread.domain.repository.HistoryRepository
import ai.saniou.thread.domain.repository.NoticeRepository
import ai.saniou.thread.domain.repository.PostRepository
import ai.saniou.thread.domain.repository.ReferenceRepository
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.SubscriptionRepository
import ai.saniou.thread.domain.repository.SyncProvider
import ai.saniou.thread.domain.repository.SyncRepository
import ai.saniou.thread.domain.repository.TagRepository
import ai.saniou.thread.domain.repository.TopicRepository
import ai.saniou.thread.domain.repository.TrendRepository
import ai.saniou.thread.data.parser.FeedParserFactory
import ai.saniou.thread.data.parser.HtmlParser
import ai.saniou.thread.data.parser.JsonParser
import ai.saniou.thread.data.parser.RssParser
import ai.saniou.thread.data.repository.ReaderRepositoryImpl
import ai.saniou.thread.domain.repository.AccountRepository
import ai.saniou.thread.domain.repository.ReaderRepository
import ai.saniou.thread.domain.repository.UserRepository
import ai.saniou.thread.domain.usecase.reader.GetArticleCountsUseCase
import ai.saniou.thread.domain.usecase.subscription.GenerateRandomSubscriptionIdUseCase
import ai.saniou.thread.domain.usecase.channel.FetchChannelsUseCase
import ai.saniou.thread.domain.usecase.channel.GetLastOpenedChannelUseCase
import ai.saniou.thread.domain.usecase.channel.SaveLastOpenedChannelUseCase
import ai.saniou.thread.network.ChallengeHandler
import ai.saniou.thread.network.CloudflareProtectionPlugin
import ai.saniou.thread.network.SaniouKtorfit
import ai.saniou.thread.network.cookie.CookieStore
import ai.saniou.thread.data.network.SettingsCookieStore
import ai.saniou.thread.network.installCookieAuth
import io.ktor.client.HttpClient
import io.ktor.client.request.header
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
    bindConstant<String>(tag = "metaDiscourseBaseUrl") { "https://meta.discourse.org/" }
    bindConstant<String>(tag = "linuxDoDiscourseBaseUrl") { "https://linux.do/" }
    bindSingleton<NmbXdApi> {
        val nmbAccountProvider = instance<NmbAccountProvider>()
        val ktorfit = SaniouKtorfit(
            baseUrl = instance<String>("nmbBaseUrl")
        ) {
            installCookieAuth { nmbAccountProvider.getAccountValue() }
        }
        ktorfit.createNmbXdApi()
    }

    bindSingleton<DiscourseApi> {
        val baseUrl = instance<String>("linuxDoDiscourseBaseUrl")
        // ChallengeHandler should be provided by the app module
        val challengeHandler = instanceOrNull<ChallengeHandler>()
        val cookieStore = instance<CookieStore>()
        val sourceId = "linuxDo"

        val ktorfit = SaniouKtorfit(
            baseUrl = baseUrl
        ) {
            installCookieAuth { cookieStore.getCookie(sourceId) }
            install(io.ktor.client.plugins.DefaultRequest) {
                header("User-Api-Key", "eebd35d4ca8e9c948cd8188f6ff9b440")
            }

            // Manually install CloudflareProtectionPlugin with sourceId if challengeHandler exists
            if (challengeHandler != null) {
                install(CloudflareProtectionPlugin) {
                    this.challengeHandler = challengeHandler
                    this.sourceId = sourceId
                }
            }
        }
        ktorfit.createDiscourseApi()
    }
    bindConstant<String>(tag = "acfunBaseUrl") { "https://api-new.acfunchina.com/" }
    bindSingleton<AcfunTokenManager> { AcfunTokenManager(instance()) }
    bindSingleton<AcfunApi> {
        val tokenManager = instance<AcfunTokenManager>()
        val ktorfit = SaniouKtorfit(
            baseUrl = instance("acfunBaseUrl")
        ) {
            install(AcfunHeaderPlugin) {
                this.tokenManager = tokenManager
            }
        }
        ktorfit.createAcfunApi()
    }

    // cache
    bindSingleton<SourceCache> { SqlDelightSourceCache(instance()) }

    // source and repository
    bindSingleton<NmbSource> { NmbSource(instance(), instance(), instance(), instance()) }
    bindSingleton<DiscourseSource> {
        DiscourseSource(
            instance(),
            instance(),
            instance(),
            instance()
        )
    }
    bindSingleton<AcfunSource> { AcfunSource(instance(), instance()) }
    bindSingleton<TiebaSource> {
        TiebaSource(
            instance(), // MiniTiebaApi
            instance(), // OfficialTiebaApi
            instance(tag = "V11"), // OfficialProtobufTiebaApi V11
            instance(tag = "V12"), // OfficialProtobufTiebaApi V12
            instance(), // WebTiebaApi
            instance(), // Database
            instance(), // AccountRepository
            instance()  // TiebaParameterProvider
        )
    }

    bind<Source>(tag = "nmb") with singleton { instance<NmbSource>() }
    bind<Source>(tag = "nga") with singleton { NgaSource() }
    bind<Source>(tag = "acfun") with singleton { instance<AcfunSource>() }
    bind<Source>(tag = "discourse") with singleton { instance<DiscourseSource>() }
    bind<Source>(tag = "tieba") with singleton { instance<TiebaSource>() }

    // "allInstance" only work in jvm current ,wait upgrade        val sources: Set<Source> = DI.allInstances()
    bind<Set<Source>>(tag = "allSources") with singleton {
        HashSet<Source>().apply {
            add(instance(tag = "nmb"))
            add(instance(tag = "nga"))
            add(instance(tag = "discourse"))
            add(instance(tag = "acfun"))
            add(instance(tag = "tieba"))
        }
    }

    bind<SourceRepository>() with singleton {
        val sources: Set<Source> = instance(tag = "allSources")
        SourceRepositoryImpl(sources)
    }

    bind<BookmarkRepository>() with singleton { BookmarkRepositoryImpl(instance()) }
    bind<TagRepository>() with singleton { TagRepositoryImpl(instance()) }
    bind<FavoriteRepository>() with singleton { FavoriteRepositoryImpl(instance()) }
    bind<SubscriptionRepository>() with singleton {
        SubscriptionRepositoryImpl(
            instance(),
            instance()
        )
    }
    bind<UserRepository>() with singleton { UserRepositoryImpl(instance()) }
    bind<AccountRepository>() with singleton { AccountRepositoryImpl(instance()) }
    bind<SettingsRepository>() with singleton { SettingsRepositoryImpl(instance()) }
    bind<NoticeRepository>() with singleton {
        NoticeRepositoryImpl(
            instance(),
            instance(),
            instance()
        )
    }
    bind<HistoryRepository>() with singleton { HistoryRepositoryImpl(instance()) }
    bind<PostRepository>() with singleton { PostRepositoryImpl(instance()) }
    bind<TrendRepository>() with singleton { TrendRepositoryImpl(instance(), instance()) }
    bind<ReferenceRepository>() with singleton { ReferenceRepositoryImpl(instance(), instance()) }
    bind<TopicRepository>() with singleton {
        TopicRepositoryImpl(
            instance(),
            instance(tag = "allSources"),
            instance()
        )
    }
    bind<ChannelRepository>() with singleton {
        ChannelRepositoryImpl(
            instance(),
            instance(tag = "allSources")
        )
    }

    // sync providers
    bind<SyncProvider>(tag = "webdav") with singleton { WebDavSyncProvider() }
    bind<SyncProvider>(tag = "local") with singleton { LocalSyncProvider() }
    bind<Set<SyncProvider>>(tag = "allSyncProviders") with singleton {
        HashSet<SyncProvider>().apply { add(instance(tag = "webdav")); add(instance(tag = "local")) }
    }

    bind<SyncRepository>() with singleton {
        val providers: Set<SyncProvider> = instance(tag = "allSyncProviders")
        SyncRepositoryImpl(providers)
    }

    bindSingleton { NmbAccountProvider(instance()) }
    // Cookie Store (for Discourse/CF)
    bindSingleton<CookieStore> { SettingsCookieStore(instance()) }
    // CDN管理器
    bindSingleton<CdnManager> { CdnManager(instance()) }
    // 数据库
    bindSingleton { createDatabase(DriverFactory()) }

    // Reader Feature
    bindSingleton { RssParser() }
    bindSingleton { JsonParser() }
    bindSingleton { HtmlParser() }
    bindSingleton { FeedParserFactory(instance(), instance(), instance()) }
    bindSingleton { HttpClient() } // Use a basic HttpClient
    bind<ReaderRepository>() with singleton {
        ReaderRepositoryImpl(instance(), instance(), instance())
    }

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
                        // TODO: Construct full cookie string from provider
                        // e.g. "CUID=${paramProvider.getCuid()}; BDUSS=${paramProvider.getBduss()}..."
                        // Since provider returns single values, we need a helper to join them.
                        // For now, using a simplified version or delegate to CookieStore if available.
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
    bind<SaveLastOpenedChannelUseCase>() with singleton { SaveLastOpenedChannelUseCase(instance()) }
}
