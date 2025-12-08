package ai.saniou.thread.data.di

import ai.saniou.thread.data.repository.FavoriteRepositoryImpl
import ai.saniou.thread.data.repository.FeedRepositoryImpl
import ai.saniou.thread.data.repository.SyncRepositoryImpl
import ai.saniou.thread.data.source.nga.NgaSource
import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.createNmbXdApi
import ai.saniou.thread.data.sync.local.LocalSyncProvider
import ai.saniou.thread.data.sync.webdav.WebDavSyncProvider
import ai.saniou.thread.domain.repository.FavoriteRepository
import ai.saniou.thread.domain.repository.FeedRepository
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.SyncProvider
import ai.saniou.thread.domain.repository.SyncRepository
import de.jensklingenberg.ktorfit.Ktorfit
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

    bind<FavoriteRepository>() with singleton { FavoriteRepositoryImpl(instance()) }

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

}
