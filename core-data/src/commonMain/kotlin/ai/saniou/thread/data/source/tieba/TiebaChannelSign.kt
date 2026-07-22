package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.source.tieba.model.MSignBean
import ai.saniou.thread.data.source.tieba.model.SignResultBean
import ai.saniou.thread.data.source.tieba.remote.MiniTiebaApi
import ai.saniou.thread.data.source.tieba.remote.OfficialTiebaApi
import ai.saniou.thread.data.source.tieba.remote.WebTiebaApi
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Channel

/**
 * Per-forum sign + multi-sign (mSign) for favorite Tieba channels.
 */
class TiebaChannelSign(
    private val miniApi: MiniTiebaApi,
    private val officialApi: OfficialTiebaApi,
    private val webApi: WebTiebaApi,
    private val database: Database,
    private val parameterProvider: TiebaParameterProvider,
) {
    val sourceId: String = TiebaMapper.SOURCE_ID

    suspend fun sign(channel: Channel): String {
        require(channel.sourceId == sourceId || channel.sourceName.equals(TiebaMapper.SOURCE_NAME, ignoreCase = true)) {
            "仅支持贴吧签到"
        }
        val tbs = parameterProvider.ensureTbs(webApi)
        val response = miniApi.sign(forumName = channel.name, tbs = tbs)
        response.ensureOk()
        val info = response.userInfo
        val streak = info?.contSignNum?.takeIf(String::isNotBlank)
        val bonus = info?.signBonusPoint?.takeIf(String::isNotBlank)
        val label = channel.displayName?.takeIf(String::isNotBlank) ?: channel.name
        return buildString {
            append(label)
            append(" 签到成功")
            if (!streak.isNullOrBlank()) append(" · 连续 $streak 天")
            if (!bonus.isNullOrBlank()) append(" · +$bonus 经验")
        }
    }

    /**
     * One-tap sign for currently favorited Tieba forums.
     * Tries official mSign first, then sequential mini sign for leftovers.
     */
    suspend fun signFavorites(): String {
        val favorites = database.favoriteChannelQueries
            .getAllFavoriteChannel(sourceId)
            .executeAsList()
            .map { it.toDomain(database.channelQueries) }
        if (favorites.isEmpty()) return "没有可签到的关注吧"

        val tbs = parameterProvider.ensureTbs(webApi)
        val uid = parameterProvider.getUid().ifBlank { return sequentialSignAll(favorites, tbs) }
        val stoken = parameterProvider.getSToken()
        val forumIds = favorites.map(Channel::id).filter(String::isNotBlank).joinToString(",")
        var mSigned = 0
        var mFailed = 0
        if (forumIds.isNotBlank() && stoken.isNotBlank()) {
            runCatching {
                val response = officialApi.mSign(
                    forumIds = forumIds,
                    tbs = tbs,
                    stoken = stoken,
                    userId = uid,
                )
                response.ensureOk()
                response.info.forEach { info ->
                    if (info.signed == "1") mSigned++ else mFailed++
                }
            }.onFailure {
                // fall through to sequential
                mSigned = 0
                mFailed = 0
            }
        }

        var sequentialOk = 0
        var sequentialFail = 0
        // If mSign already covered everything, skip sequential.
        if (mSigned + mFailed < favorites.size) {
            for (channel in favorites) {
                runCatching {
                    miniApi.sign(forumName = channel.name, tbs = tbs).ensureOk()
                    sequentialOk++
                }.onFailure { sequentialFail++ }
            }
        }

        return buildString {
            append("签到完成")
            if (mSigned > 0) append(" · 一键 $mSigned")
            if (sequentialOk > 0) append(" · 逐吧 $sequentialOk")
            val failed = mFailed + sequentialFail
            if (failed > 0) append(" · 失败 $failed")
            if (mSigned == 0 && sequentialOk == 0 && failed == 0) append(" · 无结果")
        }
    }

    private suspend fun sequentialSignAll(channels: List<Channel>, tbs: String): String {
        var ok = 0
        var fail = 0
        for (channel in channels) {
            runCatching {
                miniApi.sign(forumName = channel.name, tbs = tbs).ensureOk()
                ok++
            }.onFailure { fail++ }
        }
        return buildString {
            append("签到完成 · 逐吧 $ok")
            if (fail > 0) append(" · 失败 $fail")
        }
    }
}

internal fun SignResultBean.ensureOk() {
    if (!errorCode.isNullOrBlank() && errorCode != "0") {
        throw IllegalStateException(errorMsg?.takeIf(String::isNotBlank) ?: "签到失败 ($errorCode)")
    }
}

internal fun MSignBean.ensureOk() {
    if (errorCode.isNotBlank() && errorCode != "0") {
        val msg = error.usermsg.takeIf(String::isNotBlank)
            ?: error.errmsg.takeIf(String::isNotBlank)
            ?: "一键签到失败 ($errorCode)"
        throw IllegalStateException(msg)
    }
}
