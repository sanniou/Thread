package ai.saniou.thread.domain.model.sync

data class WebDavConfig(
    val endpoint: String,
    val username: String = "",
    val password: String = "",
) {
    init {
        require(endpoint.startsWith("https://") || endpoint.startsWith("http://")) {
            "WebDAV endpoint must use http:// or https://"
        }
    }
}

data class SyncContentSummary(
    val sourceCount: Int,
    val feedSourceCount: Int,
    val bookmarkCount: Int,
    val articleStateCount: Int,
    val settingCount: Int,
)

data class UserDataExport(
    val payload: String,
    val exportedAtEpochMillis: Long,
    val summary: SyncContentSummary,
)

data class UserDataImportReport(
    val importedAtEpochMillis: Long,
    val summary: SyncContentSummary,
    val sourceIds: Set<String>,
    val feedSourceIds: Set<String>,
)
