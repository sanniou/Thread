package ai.saniou.thread.data.platform

import android.content.Context

/**
 * Android platform bridge owned by the application entry point.
 *
 * Shared data code remains context-free; Android-only drivers and storage resolve the
 * application context through this bridge after [initialize] is called by androidApp.
 */
object AndroidPlatformContext {
    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun requireContext(): Context = applicationContext
        ?: error("AndroidPlatformContext must be initialized from Application.onCreate()")
}
