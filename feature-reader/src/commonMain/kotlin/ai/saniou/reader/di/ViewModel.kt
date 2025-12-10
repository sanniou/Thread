package ai.saniou.reader.di

import ai.saniou.reader.workflow.articledetail.ArticleDetailViewModel
import ai.saniou.reader.workflow.reader.ReaderViewModel
import org.kodein.di.DI
import org.kodein.di.bindFactory
import org.kodein.di.bindProvider
import org.kodein.di.instance

val readerViewModelModule = DI.Module("readerViewModelModule") {
    bindProvider {
        ReaderViewModel(
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance()
        )
    }
    bindFactory<String, ArticleDetailViewModel> { articleId: String ->
        ArticleDetailViewModel(articleId, instance())
    }
}