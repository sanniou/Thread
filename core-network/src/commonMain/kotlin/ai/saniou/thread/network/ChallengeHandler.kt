package ai.saniou.thread.network

/**
 * Interface for handling Cloudflare challenges (e.g., Turnstile, JS Challenge).
 * This usually involves invoking UI components (WebView) to let the user solve the challenge.
 */
interface ChallengeHandler {
    /**
     * Request to handle a Cloudflare challenge.
     *
     * @param url The URL that triggered the challenge.
     * @return `true` if the challenge was successfully passed (and cookies updated), `false` otherwise.
     */
    suspend fun handleChallenge(url: String): Boolean
}