package ai.saniou.reader.di

import org.kodein.di.DI

val readerModule = DI.Module("readerModule") {
    import(readerViewModelModule)
}