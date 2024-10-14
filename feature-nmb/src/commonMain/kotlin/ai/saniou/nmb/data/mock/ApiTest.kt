package ai.saniou.nmb.data.mock

import ai.saniou.nmb.data.api.createExampleApi
import de.jensklingenberg.ktorfit.Ktorfit

class ApiTest {
    suspend fun t(): String {
        val ktorfit = Ktorfit.Builder().baseUrl("https://swapi.dev/api/").build()
        val exampleApi = ktorfit.createExampleApi()

        val response = exampleApi.getPerson()
        println(response)

        return response;
    }
}
