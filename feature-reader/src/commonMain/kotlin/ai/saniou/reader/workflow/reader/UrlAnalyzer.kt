package ai.saniou.reader.workflow.reader

import ai.saniou.thread.domain.model.reader.FeedType

/**
 * 一个简单的 URL 分析器，根据 URL 的后缀或模式快速判断订阅源类型。
 */
class UrlAnalyzer {

    /**
     * 分析给定的 URL 字符串并返回推断的 FeedType。
     *
     * @param url 要分析的 URL。
     * @return 推断出的 [FeedType]。默认为 [FeedType.HTML]。
     */
    fun analyze(url: String): FeedType {
        val lowerCaseUrl = url.lowercase()
        return when {
            lowerCaseUrl.endsWith(".rss") || lowerCaseUrl.endsWith(".xml") -> FeedType.RSS
            lowerCaseUrl.endsWith(".json") -> FeedType.JSON
            else -> FeedType.HTML // 默认为 HTML，后续可进行更复杂的分析
        }
    }
}