package ai.saniou.nmb.data.repository

sealed interface DataPolicy {
    object LOCAL_ONLY      : DataPolicy
    object NETWORK_ONLY    : DataPolicy          // 不落库，直接走自定义 PagingSource
    object LOCAL_FIRST     : DataPolicy          // RemoteMediator：Room + API
}
