package ai.saniou.coreui.state

import androidx.paging.LoadState
import kotlin.test.Test
import kotlin.test.assertEquals

class ReliabilityStateTest {

    @Test
    fun cachedRowsRemainPrimaryDuringRefreshFailure() {
        val failure = LoadState.Error(IllegalStateException("UnknownHostException"))

        assertEquals(PagingContentState.CachedContent, resolvePagingContentState(12, failure))
        assertEquals(PagingContentState.BlockingError, resolvePagingContentState(0, failure))
    }

    @Test
    fun initialAndEmptyStatesAreStable() {
        assertEquals(PagingContentState.InitialLoading, resolvePagingContentState(0, LoadState.Loading))
        assertEquals(
            PagingContentState.Empty,
            resolvePagingContentState(0, LoadState.NotLoading(endOfPaginationReached = true)),
        )
    }

    @Test
    fun refreshFailuresUseTheSharedUserVocabulary() {
        assertEquals(AppErrorType.NETWORK, IllegalStateException("UnknownHostException").toAppError().type)
        assertEquals(AppErrorType.AUTHENTICATION, IllegalStateException("HTTP 401 unauthorized").toAppError().type)
        assertEquals(AppErrorType.RATE_LIMIT, IllegalStateException("HTTP 429").toAppError().type)
        assertEquals(AppErrorType.SERVER, IllegalStateException("HTTP 503 server error").toAppError().type)
    }
}
