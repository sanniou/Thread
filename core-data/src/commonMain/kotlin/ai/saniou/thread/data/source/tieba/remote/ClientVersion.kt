package ai.saniou.thread.data.source.tieba.remote

enum class ClientVersion(val version: String) {
    TIEBA_V11("11.10.8.6"),
    TIEBA_V12("12.60.1.0"),
    TIEBA_V12_POST("12.59.1.0"); // Typically same as V12 but different usage context in TiebaLite
}