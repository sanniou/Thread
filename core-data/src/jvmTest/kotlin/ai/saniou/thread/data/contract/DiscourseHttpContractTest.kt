package ai.saniou.thread.data.contract

import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.discourse.remote.createDiscourseApi
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseLatestPostsResponse
import ai.saniou.thread.domain.refresh.FailureClassifier
import ai.saniou.thread.domain.refresh.RefreshFailureKind
import ai.saniou.thread.network.HttpStatusException
import ai.saniou.thread.network.SaniouHttpClient
import ai.saniou.thread.network.SaniouKtorfit
import ai.saniou.thread.network.SaniouResult
import io.ktor.client.HttpClient
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DiscourseHttpContractTest {
    @Test
    fun replaysSuccessAndRecordsRequestContract() = runBlocking {
        contract(
            FixtureExchange(HttpMethod.Get, "/latest.json", 200, "discourse/latest-success.json"),
        ).use { contract ->
            val response = assertIs<SaniouResult.Success<DiscourseLatestPostsResponse>>(
                contract.api.getLatestTopics()
            )
            assertEquals(1, response.data.topicList.topics.size)
            assertEquals("Offline-first topic", response.data.topicList.topics.single().title)
            assertEquals("page=0", contract.harness.requests.single().query)
            assertTrue(HttpHeaders.Authorization !in contract.harness.requests.single().headerNames)
            contract.harness.assertExhausted()
        }
    }

    @Test
    fun preservesAuthenticationAndRateLimitMetadata() = runBlocking {
        contract(
            FixtureExchange(HttpMethod.Get, "/latest.json", 401, "discourse/authentication-expired.json"),
            FixtureExchange(
                HttpMethod.Get,
                "/categories.json",
                429,
                "discourse/rate-limited.json",
                responseHeaders = mapOf(HttpHeaders.RetryAfter to "15"),
            ),
        ).use { contract ->
            val auth = contract.api.getLatestTopics().statusFailure()
            val limited = contract.api.getCategories().statusFailure()

            assertEquals(401, auth.statusCode)
            assertEquals(RefreshFailureKind.AUTHENTICATION, FailureClassifier.classify(auth))
            assertEquals(429, limited.statusCode)
            assertEquals(15, limited.retryAfterSeconds)
            assertEquals(RefreshFailureKind.RATE_LIMIT, FailureClassifier.classify(limited))
            contract.harness.assertExhausted()
        }
    }

    @Test
    fun uploadLimitIsStructuredAndIndependentCallsPartiallySucceed() = runBlocking {
        contract(
            FixtureExchange(HttpMethod.Post, "/uploads.json", 413, "discourse/upload-too-large.json"),
            FixtureExchange(HttpMethod.Get, "/latest.json", 200, "discourse/latest-success.json"),
            FixtureExchange(HttpMethod.Get, "/categories.json", 503, "discourse/service-unavailable.json"),
        ).use { contract ->
            val upload = contract.api.upload(parts = emptyList()).statusFailure()
            assertEquals(413, upload.statusCode)
            assertTrue("too large" in upload.responseBody.lowercase())

            val (latest, categories) = coroutineScope {
                val latest = async { contract.api.getLatestTopics() }
                val categories = async { contract.api.getCategories() }
                latest.await() to categories.await()
            }
            assertIs<SaniouResult.Success<*>>(latest)
            val unavailable = categories.statusFailure()
            assertEquals(503, unavailable.statusCode)
            assertEquals(RefreshFailureKind.REMOTE, FailureClassifier.classify(unavailable))
            assertEquals(3, contract.harness.requests.size)
            contract.harness.assertExhausted()
        }
    }

    private fun contract(vararg exchanges: FixtureExchange): ContractClient {
        val harness = ConnectorContractHarness(*exchanges)
        val client = SaniouHttpClient(harness.engine)
        return ContractClient(
            harness = harness,
            client = client,
            api = SaniouKtorfit("https://fixture.thread.test/", client).createDiscourseApi(),
        )
    }
}

private data class ContractClient(
    val harness: ConnectorContractHarness,
    val client: HttpClient,
    val api: DiscourseApi,
) : AutoCloseable {
    override fun close() = client.close()
}

private fun SaniouResult<*>.statusFailure(): HttpStatusException =
    assertIs<HttpStatusException>(assertIs<SaniouResult.Error>(this).ex)
