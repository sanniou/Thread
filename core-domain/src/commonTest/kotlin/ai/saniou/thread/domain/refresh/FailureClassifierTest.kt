package ai.saniou.thread.domain.refresh

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FailureClassifierTest {
    @Test
    fun classifiesErrorsForRetryAndUiFromOneVocabulary() {
        assertEquals(RefreshFailureKind.TIMEOUT, FailureClassifier.classify(IllegalStateException("request timed out")))
        assertEquals(RefreshFailureKind.RATE_LIMIT, FailureClassifier.classify(IllegalStateException("HTTP 429")))
        assertEquals(RefreshFailureKind.AUTHENTICATION, FailureClassifier.classify(IllegalStateException("401 unauthorized")))
        assertEquals(RefreshFailureKind.OFFLINE, FailureClassifier.classify(IllegalStateException("UnknownHostException")))
        assertEquals(RefreshFailureKind.REMOTE, FailureClassifier.classify(IllegalStateException("503 server error")))
        assertEquals(RefreshFailureKind.UNKNOWN, FailureClassifier.classify(IllegalStateException("invalid selector")))
    }

    @Test
    fun sanitizesCredentialShapedDiagnosticFragments() {
        val sanitized = DiagnosticSanitizer.sanitize(
            "401 Bearer abc.def token=very-secret cookie:session-value https://host/path?api_key=query-secret&safe=also-hidden"
        ).orEmpty()

        assertTrue("[REDACTED]" in sanitized)
        listOf("abc.def", "very-secret", "session-value", "query-secret", "also-hidden").forEach {
            assertFalse(it in sanitized)
        }
    }
}
