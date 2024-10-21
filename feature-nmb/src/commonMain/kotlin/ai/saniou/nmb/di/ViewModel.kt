package ai.saniou.nmb.di


import androidx.lifecycle.ViewModel
import org.kodein.di.DI
import org.kodein.di.bind


//inline fun <reified VM : ViewModel, T> T.activityViewModel(): Lazy<VM> where T : DIAware, T : FragmentActivity {
//    return viewModels(factoryProducer = { direct.instance() })
//}
//
//inline fun <reified VM : ViewModel, T> T.activityScopedFragmentViewModel(): Lazy<VM> where T : DIAware, T : Fragment {
//    return viewModels(ownerProducer = { requireParentFragment() },
//        factoryProducer = { getFactoryInstance() })
//}
//
//inline fun <reified VM : ViewModel, T> T.fragmentViewModel(): Lazy<VM> where T : DIAware, T : Fragment {
//    return viewModels(factoryProducer = { getFactoryInstance() })
//}

inline fun <reified VM : ViewModel> DI.Builder.bindViewModel(overrides: Boolean? = null): DI.Builder.TypeBinder<VM> {
    return bind<VM>(VM::class.simpleName, overrides)
}

//fun <T> T.getFactoryInstance(
//): ViewModelProvider.Factory where T : DIAware, T : Fragment {
//    val viewModeFactory: ViewModelProvider.Factory by DI.on(activity).instance()
//    return viewModeFactory
//}
