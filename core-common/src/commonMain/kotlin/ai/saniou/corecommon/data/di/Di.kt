package ai.saniou.corecommon.data.di

import ai.saniou.corecommon.data.CookieProvider
import ai.saniou.corecommon.data.SaniouKtorfit
import de.jensklingenberg.ktorfit.Ktorfit
import org.kodein.di.DI
import org.kodein.di.bindMultiton
import org.kodein.di.instanceOrNull

val coreCommon by DI.Module {

    bindMultiton<String, Ktorfit> { baseUrl ->
        SaniouKtorfit(baseUrl, instanceOrNull())
    }

//    bindSingleton<Ktorfit> {
//        ktorfit(this.instance<String>("baseUrl"))
//    }

}
