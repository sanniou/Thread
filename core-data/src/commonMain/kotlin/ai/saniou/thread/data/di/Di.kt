package ai.saniou.thread.data.di

import ai.saniou.thread.data.repository.FeedRepositoryImpl
import ai.saniou.thread.data.repository.SyncRepositoryImpl
import ai.saniou.thread.data.source.nga.NgaSource
import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.thread.data.source.nmb.remote.NmbApi
import ai.saniou.thread.data.sync.local.LocalSyncProvider
import ai.saniou.thread.data.sync.webdav.WebDavSyncProvider
import ai.saniou.thread.domain.repository.FeedRepository
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.SyncProvider
import ai.saniou.thread.domain.repository.SyncRepository
import de.jensklingenberg.ktorfit.Ktorfit
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

val dataModule = DI.Module("dataModule") {
    bind<NmbApi>() with singleton {
        val ktorfit: Ktorfit = instance(arg = "https://api.nmb.best/api/")
        ktorfit.create()
    }

    bind<Source>(tag = "nmb") with singleton { NmbSource(instance()) }
    bind<Source>(tag = "nga") with singleton { NgaSource() }

    bind<FeedRepository>() with singleton {
        val sources: Set<Source> = instance()
        FeedRepositoryImpl(sources)
    }

//    bind<Set<Source>>() with set<Source>()

    bind<SyncProvider>(tag = "webdav") with singleton { WebDavSyncProvider() }
    bind<SyncProvider>(tag = "local") with singleton { LocalSyncProvider() }

    bind<SyncRepository>() with singleton {
        val providers: Set<SyncProvider> = instance()
        SyncRepositoryImpl(providers)
    }

//    bind<Set<SyncProvider>>() with set<SyncProvider>()
}
