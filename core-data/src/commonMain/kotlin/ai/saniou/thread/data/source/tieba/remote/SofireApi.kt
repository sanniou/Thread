package ai.saniou.thread.data.source.tieba.remote

import ai.saniou.thread.data.source.tieba.model.SofireResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.HeaderMap
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Query
import de.jensklingenberg.ktorfit.http.Url

/**
 * 移植自 com.huanchengfly.tieba.post.api.retrofit.interfaces.SofireApi
 */
interface SofireApi {
    /**
     * Sofire 签名/校验接口 (通常用于安全验证)
     */
    @POST
    suspend fun post(
        @Url url: String,
        @Query("skey") skey: String,
        @Body body: String, // Using String for RequestBody placeholder
        @HeaderMap headers: Map<String, String>,
    ): SofireResponse
}
