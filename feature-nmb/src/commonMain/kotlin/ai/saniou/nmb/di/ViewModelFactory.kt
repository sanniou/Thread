package ai.saniou.nmb.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import org.kodein.di.DI
import org.kodein.di.instanceOrNull
import kotlin.reflect.KClass

class ViewModelFactory(private val injector: DI) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        return injector.instanceOrNull<ViewModel>(tag = modelClass.simpleName) as T?
            ?: throw IllegalArgumentException("ViewModel ${modelClass.simpleName} not found")
    }
}
