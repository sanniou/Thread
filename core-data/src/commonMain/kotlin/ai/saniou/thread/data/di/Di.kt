package ai.saniou.thread.data.di
import ai.saniou.thread.data.database.DriverFactory
import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.data.manager.CdnManager
import ai.saniou.thread.data.repository.BookmarkRepositoryImpl
import ai.saniou.thread.data.repository.FavoriteRepositoryImpl
import ai.saniou.thread.data.repository.SourceRepositoryImpl
import ai.saniou.thread.data.repository.ForumRepositoryImpl
import ai.saniou.thread.data.repository.HistoryRepositoryImpl
import ai.saniou.thread.data.repository.NoticeRepositoryImpl
import ai.saniou.thread.data.repository.PostRepositoryImpl
import ai.saniou.thread.data.repository.ReferenceRepositoryImpl
import ai.saniou.thread.data.repository.SettingsRepositoryImpl
import ai.saniou.thread.data.repository.SubscriptionRepositoryImpl
import ai.saniou.thread.data.repository.SyncRepositoryImpl
import ai.saniou.thread.data.repository.ThreadRepositoryImpl
import ai.saniou.thread.data.repository.TagRepositoryImpl
import ai.saniou.thread.data.repository.TrendRepositoryImpl
import ai.saniou.thread.data.repository.UserRepositoryImpl
import ai.saniou.thread.data.source.nga.NgaSource
import ai.saniou.thread.data.source.nmb.NmbCookieProvider
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
import ai.saniou.thread.domain.repository.ForumRepository
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
import ai.saniou.thread.domain.repository.ThreadRepository
import ai.saniou.thread.domain.repository.TrendRepository
import ai.saniou.thread.data.parser.FeedParserFactory
import ai.saniou.thread.data.parser.HtmlParser
import ai.saniou.thread.data.parser.JsonParser
import ai.saniou.thread.data.parser.RssParser
import ai.saniou.thread.data.repository.ReaderRepositoryImpl
import ai.saniou.thread.domain.repository.ReaderRepository
import ai.saniou.thread.domain.repository.UserRepository
import ai.saniou.thread.domain.usecase.reader.GetArticleCountsUseCase
import ai.saniou.thread.network.CookieProvider
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.bindConstant
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import org.kodein.di.singleton

val dataModule = DI.Module("dataModule") {
    bindConstant<String>(tag = "nmbBaseUrl") { "https://api.nmb.best/api/" }
    bindConstant<String>(tag = "discourseBaseUrl") { "https://meta.discourse.org/" }

    bindSingleton<NmbXdApi> {
        val ktorfit: Ktorfit = instance(arg = instance<String>("nmbBaseUrl"))
        ktorfit.createNmbXdApi()
    }

    bindSingleton<DiscourseApi> {
        val ktorfit: Ktorfit = instance(arg = instance<String>("discourseBaseUrl"))
        ktorfit.createDiscourseApi()
    }

    // cache
    bindSingleton<SourceCache> { SqlDelightSourceCache(instance()) }

    // source and repository
    bindSingleton<NmbSource> { NmbSource(instance(), instance(), instance()) }
    bindSingleton<DiscourseSource> { DiscourseSource(instance(), instance(), instance()) }
    
    bind<Source>(tag = "nmb") with singleton { instance<NmbSource>() }
    bind<Source>(tag = "nga") with singleton { NgaSource() }
    bind<Source>(tag = "discourse") with singleton { instance<DiscourseSource>() }

    // "allInstance" only work in jvm current ,wait upgrade        val sources: Set<Source> = DI.allInstances()
    bind<Set<Source>>(tag = "allSources") with singleton {
        HashSet<Source>().apply {
            add(instance(tag = "nmb"))
            add(instance(tag = "nga"))
            add(instance(tag = "discourse"))
        }
    }

    bind<SourceRepository>() with singleton {
        val sources: Set<Source> = instance(tag = "allSources")
        SourceRepositoryImpl(sources)
    }

    bind<BookmarkRepository>() with singleton { BookmarkRepositoryImpl(instance()) }
    bind<TagRepository>() with singleton { TagRepositoryImpl(instance()) }
    bind<FavoriteRepository>() with singleton { FavoriteRepositoryImpl(instance()) }
    bind<SubscriptionRepository>() with singleton { SubscriptionRepositoryImpl(instance(), instance()) }
    bind<UserRepository>() with singleton { UserRepositoryImpl(instance()) }
    bind<SettingsRepository>() with singleton { SettingsRepositoryImpl(instance()) }
    bind<NoticeRepository>() with singleton { NoticeRepositoryImpl(instance(), instance(), instance()) }
    bind<HistoryRepository>() with singleton { HistoryRepositoryImpl(instance()) }
    bind<PostRepository>() with singleton { PostRepositoryImpl(instance()) }
    bind<TrendRepository>() with singleton { TrendRepositoryImpl(instance()) }
    bind<ReferenceRepository>() with singleton { ReferenceRepositoryImpl(instance(), instance()) }
    bind<ThreadRepository>() with singleton {
        ThreadRepositoryImpl(
            instance(),
            instance(tag = "allSources"),
            instance()
        )
    }
    bind<ForumRepository>() with singleton {
        ForumRepositoryImpl(
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

    bindSingleton<CookieProvider> { NmbCookieProvider(instance()) }
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
    bind<GetArticleCountsUseCase>() with singleton { GetArticleCountsUseCase(instance()) }
}
