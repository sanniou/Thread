package ai.saniou.thread.di

import ai.saniou.thread.feature.bookmark.BookmarkViewModel
import ai.saniou.thread.feature.challenge.UiChallengeHandler
import ai.saniou.thread.feature.history.HistoryViewModel
import ai.saniou.thread.feature.settings.SyncSettingsViewModel
import ai.saniou.thread.feature.search.GlobalSearchViewModel
import ai.saniou.thread.feature.operations.OperationsViewModel
import ai.saniou.thread.feature.activity.ActivityCenterViewModel
import ai.saniou.thread.feature.inbox.InboxViewModel
import ai.saniou.thread.network.ChallengeHandler
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val appModule = DI.Module("appModule") {
    bindSingleton<UiChallengeHandler> { UiChallengeHandler(instance()) }
    bindSingleton<ChallengeHandler> { instance<UiChallengeHandler>() }
    bindProvider { HistoryViewModel(instance()) }
    bindProvider { BookmarkViewModel(instance(), instance(), instance()) }
    bindProvider { GlobalSearchViewModel(instance(), instance(), instance(), instance()) }
    bindProvider { OperationsViewModel(instance(), instance()) }
    bindProvider { ActivityCenterViewModel(instance(), instance(), instance()) }
    bindProvider { InboxViewModel(instance()) }
    bindProvider {
        SyncSettingsViewModel(
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
        )
    }
}
