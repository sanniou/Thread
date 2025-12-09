package ai.saniou.thread.domain.repository

import io.ktor.http.content.PartData

interface PostRepository {
    suspend fun post(
        fid: Int,
        content: String,
        name: String? = null,
        title: String? = null,
        water: Boolean = false,
        image: PartData? = null
    ): String

    suspend fun reply(
        resto: Int,
        content: String,
        name: String? = null,
        title: String? = null,
        water: Boolean = false,
        image: PartData? = null
    ): String
}