package ai.saniou.thread.network

import io.ktor.client.engine.HttpClientEngineFactory

expect val HttpEngineFactory: HttpClientEngineFactory<*>