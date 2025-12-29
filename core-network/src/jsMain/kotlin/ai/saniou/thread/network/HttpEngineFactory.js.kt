package ai.saniou.thread.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js

actual val HttpEngineFactory: HttpClientEngineFactory<*> = Js