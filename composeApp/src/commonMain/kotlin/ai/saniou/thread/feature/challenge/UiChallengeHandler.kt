package ai.saniou.thread.feature.challenge

import ai.saniou.thread.network.ChallengeHandler
import ai.saniou.thread.network.cookie.CookieStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UiChallengeHandler(
    private val cookieStore: CookieStore
) : ChallengeHandler {

    private val mutex = Mutex()
    private val _challengeEvents = MutableSharedFlow<ChallengeRequest>()
    val challengeEvents: SharedFlow<ChallengeRequest> = _challengeEvents.asSharedFlow()

    data class ChallengeRequest(
        val sourceId: String,
        val url: String,
        val onResult: (Boolean) -> Unit
    )

    override suspend fun handleChallenge(sourceId: String, url: String): Boolean = mutex.withLock {
        val resultDeferred = CompletableDeferred<Boolean>()

        // 1. 发送请求给 UI
        _challengeEvents.emit(ChallengeRequest(sourceId, url) { success ->
            resultDeferred.complete(success)
        })

        // 2. 等待 UI 处理结果
        return resultDeferred.await()
    }

    /**
     * Called by UI when challenge is completed (success or failure/cancelled)
     * @param sourceId The source ID associated with the challenge (passed in ChallengeRequest)
     */
    suspend fun onChallengeResult(sourceId: String, success: Boolean, cookies: String? = null) {
        if (success && cookies != null) {
            cookieStore.saveCookie(sourceId, cookies)
        }
        // This part is tricky because we need to map the result back to the specific request.
        // In the handleChallenge implementation above, we pass a callback 'onResult'.
        // The UI component consuming the event should call that callback.
    }
}