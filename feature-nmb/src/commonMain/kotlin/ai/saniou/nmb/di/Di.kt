package ai.saniou.nmb.di

import ai.saniou.corecommon.data.di.coreCommon
import ai.saniou.nmb.data.api.NmbXdApi
import ai.saniou.nmb.data.api._NmbXdApiImpl
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.domain.ForumCategoryUserCase
import ai.saniou.nmb.domain.ForumUserCase
import ai.saniou.nmb.workflow.home.ForumCategoryViewModel
import ai.saniou.nmb.workflow.home.ForumViewModel
import org.kodein.di.DI
import org.kodein.di.bindConstant
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance


val nmbdi = DI {
    import(coreCommon)

    bindConstant<String>(tag = "nmbBaseUrl") { "https://api.nmb.best/api/" }

    bindSingleton<NmbXdApi> { _NmbXdApiImpl(instance(arg = instance<String>("nmbBaseUrl"))) }

    bindSingleton<ForumRepository> { ForumRepository(instance()) }
    bindProvider<ForumCategoryUserCase> { ForumCategoryUserCase(instance()) }
    bindProvider<ForumCategoryViewModel> { ForumCategoryViewModel(instance()) }

    bindProvider { ForumUserCase(instance()) }
    bindProvider { ForumViewModel(instance()) }

}


