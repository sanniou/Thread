package ai.saniou.nmb.data.api;

import ai.saniou.corecommon.di.SaniouResponse
import ai.saniou.nmb.data.entity.CdnPath
import ai.saniou.nmb.data.entity.ForumCategory
import ai.saniou.nmb.data.entity.TimeLine
import de.jensklingenberg.ktorfit.http.GET

interface NmbXdApi {
    @GET("getCDNPath")
    suspend fun getCdnPath(): List<CdnPath>

    @GET("getTimelineList")
    suspend fun getTimelineList(): SaniouResponse<List<TimeLine>>

    @GET("getForumList")
    suspend fun getForumList(): List<ForumCategory>
}
