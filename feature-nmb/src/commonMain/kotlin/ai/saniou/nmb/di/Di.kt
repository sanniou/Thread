package ai.saniou.nmb.di

import ai.saniou.corecommon.di.coreCommon
import ai.saniou.nmb.data.api.NmbXdApi
import ai.saniou.nmb.data.api._NmbXdApiImpl
import org.kodein.di.DI
import org.kodein.di.bindConstant
import org.kodein.di.bindSingleton
import org.kodein.di.instance


val nmbdi = DI {
    import(coreCommon)

    bindConstant<String>(tag = "nmbBaseUrl") { "https://api.nmb.best/api/" }

    bindSingleton<NmbXdApi> { _NmbXdApiImpl(instance(arg = instance<String>("nmbBaseUrl"))) }
}
