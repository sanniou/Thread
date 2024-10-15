package ai.saniou.nmb.di

import ai.saniou.corecommon.di.coreCommon
import ai.saniou.nmb.data.api.ExampleApi
import ai.saniou.nmb.data.api.createExampleApi
import de.jensklingenberg.ktorfit.Ktorfit
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val nmbdi = DI {
    import(coreCommon)
    bindSingleton<ExampleApi> {
        val ktorfit = instance<Ktorfit>()
        ktorfit.createExampleApi()
    }

}
