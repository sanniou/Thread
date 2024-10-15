package ai.saniou.corecommon.di

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.kodein.di.DI
import org.kodein.di.bindSingleton

val coreCommon = DI.Module("coreCommon") {
    bindSingleton<Ktorfit> {
        ktorfit {
            baseUrl("https://swapi.dev/api/")
            httpClient(
                HttpClient {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                isLenient = true
                                ignoreUnknownKeys = true
                            }
                        )
                    }
                }
            )
//            converterFactories(
//                FlowConverterFactory(),
//                CallConverterFactory()
//            )
        }
    }

}
