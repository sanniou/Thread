package ai.saniou.corecommon.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/** Dispatcher for blocking platform I/O used from shared code. */
expect val ioDispatcher: CoroutineDispatcher
