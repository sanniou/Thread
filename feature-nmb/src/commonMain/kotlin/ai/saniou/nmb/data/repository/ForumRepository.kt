package ai.saniou.nmb.data.repository

import ai.saniou.nmb.data.api.NmbXdApi

class ForumRepository(
    private val NmbXdApi: NmbXdApi,
) : NmbXdApi by NmbXdApi {


//    fun pagedForums(
//        policy: DataPolicy,
//    ): Flow<PagingData<Forum>> {
//        val pager = when (policy) {
//            DataPolicy.LOCAL_ONLY -> Pager(
//                PagingConfig(20),
//                pagingSourceFactory = { dao.pagingSource() }
//            )
//
//            DataPolicy.NETWORK_ONLY -> Pager(
//                PagingConfig(20),
//                pagingSourceFactory = { NetworkPagingSource(api) } // ← 纯网络
//            )
//
//            DataPolicy.LOCAL_FIRST -> Pager(
//                PagingConfig(20),
//                remoteMediator = ForumRemoteMediator(),
//                pagingSourceFactory = { dao.pagingSource() }       // ← Room
//            )
//        }
//        return pager
//    }

}
