package ai.saniou.thread.data.source.tieba.remote

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Streaming
import de.jensklingenberg.ktorfit.http.Url
import io.ktor.client.statement.HttpStatement

/**
 * 移植自 com.huanchengfly.tieba.post.api.retrofit.interfaces.LiteApiInterface
 */
interface LiteApiInterface {
    /**
     * 获取壁纸列表
     * 对应 URL: https://huancheng65.github.io/TiebaLite/wallpapers.json
     */
    @GET("https://huancheng65.github.io/TiebaLite/wallpapers.json")
    suspend fun wallpapers(): List<String>

    /**
     * 流式下载 URL 内容
     *
     * @param url 目标 URL
     * @return HttpStatement 对象，需手动处理流
     */
    @Streaming
    @GET
    suspend fun streamUrl(@Url url: String): HttpStatement
}
