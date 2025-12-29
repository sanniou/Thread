package ai.saniou.thread.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

actual val HttpEngineFactory: HttpClientEngineFactory<*> = OkHttp