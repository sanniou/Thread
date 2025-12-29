package ai.saniou.thread.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual val HttpEngineFactory: HttpClientEngineFactory<*> = Darwin