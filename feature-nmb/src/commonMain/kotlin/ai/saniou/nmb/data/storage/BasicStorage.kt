package ai.saniou.nmb.data.storage

import io.github.irgaly.kottage.Kottage
import io.github.irgaly.kottage.KottageStorage
import io.github.irgaly.kottage.platform.KottageContext
import kotlinx.coroutines.CoroutineScope


abstract class BasicStorage(
    private val scope: CoroutineScope,
    private val storageName: String,
    private val databaseName: String = "nmb-storage"
) {
    private val directoryPath by lazy { getStorageDirectory() }
    private val kottage: Kottage by lazy {
        Kottage(
            name = databaseName, // This will be database file name
            directoryPath = directoryPath,
            environment = KottageEnvironment(),
            scope = scope
        )
    }


    protected val storage: KottageStorage by lazy {
        kottage.storage(storageName)
    }

    /**
     * 创建 KottageEnvironment
     */
    private fun KottageEnvironment(): io.github.irgaly.kottage.KottageEnvironment {
        return io.github.irgaly.kottage.KottageEnvironment(
            context = KottageContext()
        )
    }
}
