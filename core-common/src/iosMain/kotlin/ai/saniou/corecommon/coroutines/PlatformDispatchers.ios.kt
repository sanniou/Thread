package ai.saniou.corecommon.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Kotlin/Native does not expose Dispatchers.IO as public API.
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
