package ai.saniou.thread.di

import ai.saniou.thread.feature.challenge.UiChallengeHandler
import ai.saniou.thread.network.ChallengeHandler
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val appModule = DI.Module("appModule") {
    bindSingleton<UiChallengeHandler> { UiChallengeHandler(instance()) }
    bindSingleton<ChallengeHandler> { instance<UiChallengeHandler>() }
}