package ai.saniou.nmb.data.storage

import ai.saniou.nmb.data.entity.ForumCategory
import io.github.irgaly.kottage.Kottage
import io.github.irgaly.kottage.KottageStorage
import io.github.irgaly.kottage.getOrNull
import io.github.irgaly.kottage.platform.KottageContext
import io.github.irgaly.kottage.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days


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
