package ai.saniou.corecommon.data.di

import ai.saniou.corecommon.data.SaniouKtorfit
import de.jensklingenberg.ktorfit.Ktorfit
import org.kodein.di.DI
import org.kodein.di.bindMultiton

val coreCommon by DI.Module {

    bindMultiton<String, Ktorfit> { baseUrl ->
        SaniouKtorfit(baseUrl)
    }

//    bindSingleton<Ktorfit> {
//        ktorfit(this.instance<String>("baseUrl"))
//    }

}
