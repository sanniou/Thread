package ai.saniou.thread.data.di

import ai.saniou.nmb.data.NmbCookieProvider
import ai.saniou.nmb.data.database.DriverFactory
import ai.saniou.nmb.data.database.createDatabase
import ai.saniou.nmb.data.manager.CdnManager
import ai.saniou.nmb.data.storage.CategoryStorage
import ai.saniou.nmb.data.storage.CommonStorage
import ai.saniou.nmb.data.storage.GreetImageStorage
import ai.saniou.nmb.data.storage.SubscriptionStorage
import ai.saniou.thread.data.repository.BookmarkRepositoryImpl
import ai.saniou.thread.data.repository.FavoriteRepositoryImpl
import ai.saniou.thread.data.repository.FeedRepositoryImpl
import ai.saniou.thread.data.repository.SubscriptionRepositoryImpl
import ai.saniou.thread.data.repository.SyncRepositoryImpl
import ai.saniou.thread.data.source.nga.NgaSource
import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.createNmbXdApi
import ai.saniou.thread.data.sync.local.LocalSyncProvider
import ai.saniou.thread.data.sync.webdav.WebDavSyncProvider
import ai.saniou.thread.domain.repository.BookmarkRepository
import ai.saniou.thread.domain.repository.FavoriteRepository
import ai.saniou.thread.domain.repository.FeedRepository
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.data.repository.UserRepositoryImpl
import ai.saniou.thread.domain.repository.SubscriptionRepository
import ai.saniou.thread.domain.repository.SyncProvider
import ai.saniou.thread.data.repository.NoticeRepositoryImpl
import ai.saniou.thread.data.repository.HistoryRepositoryImpl
import ai.saniou.thread.data.repository.PostRepositoryImpl
import ai.saniou.thread.domain.repository.HistoryRepository
import ai.saniou.thread.data.repository.TrendRepositoryImpl
import ai.saniou.thread.domain.repository.NoticeRepository
import ai.saniou.thread.data.repository.ReferenceRepositoryImpl
import ai.saniou.thread.domain.repository.PostRepository
import ai.saniou.thread.domain.repository.ReferenceRepository
import ai.saniou.thread.domain.repository.TrendRepository
import ai.saniou.thread.domain.repository.SyncRepository
import ai.saniou.thread.domain.repository.UserRepository
import ai.saniou.thread.network.CookieProvider
import de.jensklingenberg.ktorfit.Ktorfit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.bindConstant
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import org.kodein.di.singleton

val dataModule = DI.Module("dataModule") {
    bindConstant<String>(tag = "nmbBaseUrl") { "https://api.nmb.best/api/" }
    bindSingleton<NmbXdApi> {
        val ktorfit: Ktorfit = instance(arg = instance<String>("nmbBaseUrl"))
        ktorfit.createNmbXdApi()
    }

    // source and repository
    bind<Source>(tag = "nmb") with singleton { NmbSource(instance(), instance()) }
    bind<Source>(tag = "nga") with singleton { NgaSource() }

    // "allInstance" only work in jvm current ,wait upgrade        val sources: Set<Source> = DI.allInstances()
    bind<Set<Source>>(tag = "allSources") with singleton {
        HashSet<Source>().apply { add(instance(tag = "nmb")); add(instance(tag = "nga")) }
    }

    bind<FeedRepository>() with singleton {
        val sources: Set<Source> = instance(tag = "allSources")
        FeedRepositoryImpl(sources)
    }

    bind<BookmarkRepository>() with singleton { BookmarkRepositoryImpl(instance()) }
    bind<FavoriteRepository>() with singleton { FavoriteRepositoryImpl(instance()) }
    bind<SubscriptionRepository>() with singleton { SubscriptionRepositoryImpl(instance(), instance()) }
    bind<UserRepository>() with singleton { UserRepositoryImpl(instance( )) }
    bind<NoticeRepository>() with singleton { NoticeRepositoryImpl(instance(), instance(), instance()) }
    bind<HistoryRepository>() with singleton { HistoryRepositoryImpl(instance()) }
    bind<PostRepository>() with singleton { PostRepositoryImpl(instance()) }
    bind<TrendRepository>() with singleton { TrendRepositoryImpl(instance()) }
    bind<ReferenceRepository>() with singleton { ReferenceRepositoryImpl(instance(), instance()) }

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
    // 数据存储相关
    bindSingleton { CategoryStorage(scope = CoroutineScope(Dispatchers.Default)) }
    // 欢迎图片存储
    bindSingleton { GreetImageStorage(scope = CoroutineScope(Dispatchers.Default)) }
    // 订阅存储
    bindSingleton { SubscriptionStorage(scope = CoroutineScope(Dispatchers.Default)) }
    bindSingleton { createDatabase(DriverFactory()) }
    bindSingleton { CommonStorage(scope = CoroutineScope(Dispatchers.Default)) }


}
