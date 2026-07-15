package ai.saniou.thread.data.refresh

import ai.saniou.thread.domain.refresh.RefreshFailureKind
import ai.saniou.thread.domain.refresh.RefreshPolicy
import ai.saniou.thread.domain.refresh.RefreshStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultRefreshCoordinatorTest {
    @Test
    fun retriesTimeoutAndPublishesRecovery() = runBlocking {
        val coordinator = DefaultRefreshCoordinator()
        var calls = 0

        val result = coordinator.execute(
            key = "reader:test",
            policy = RefreshPolicy(maxAttempts = 3, initialDelayMillis = 0, maxDelayMillis = 0),
        ) {
            calls += 1
            if (calls == 1) Result.failure(IllegalStateException("request timeout"))
            else Result.success("ok")
        }

        assertEquals("ok", result.getOrThrow())
        assertEquals(2, calls)
        assertEquals(RefreshStatus.SUCCEEDED, coordinator.states.value.getValue("reader:test").status)
        assertEquals(2, coordinator.states.value.getValue("reader:test").attempt)
    }

    @Test
    fun doesNotRetryAuthenticationFailure() = runBlocking {
        val coordinator = DefaultRefreshCoordinator()
        var calls = 0

        val result = coordinator.execute(
            key = "forum:tieba:catalog",
            policy = RefreshPolicy(maxAttempts = 3, initialDelayMillis = 0, maxDelayMillis = 0),
        ) {
            calls += 1
            Result.failure<Unit>(IllegalStateException("401 unauthorized cookie"))
        }

        assertTrue(result.isFailure)
        assertEquals(1, calls)
        val state = coordinator.states.value.getValue("forum:tieba:catalog")
        assertEquals(RefreshStatus.FAILED, state.status)
        assertEquals(RefreshFailureKind.AUTHENTICATION, state.failureKind)
    }
}
