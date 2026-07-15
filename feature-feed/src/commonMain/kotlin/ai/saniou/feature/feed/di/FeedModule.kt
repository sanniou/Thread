package ai.saniou.feature.feed.di

import ai.saniou.feature.feed.workflow.FeedViewModel
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance

val feedModule = DI.Module("feedModule") {
    bindProvider {
        FeedViewModel(
            getTimeline = instance(),
            refreshTimeline = instance(),
            getAvailableSources = instance(),
            observeRefreshDiagnostics = instance(),
        )
    }
}
