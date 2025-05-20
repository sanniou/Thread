//package ai.saniou.nmb.data.source
//
//import ai.saniou.nmb.data.database.AppDatabase
//import ai.saniou.nmb.data.entity.Forum
//import kotlin.properties.Delegates
//
//class ForumPagingSource(
//    private val db: AppDatabase,
//) : BasePagingSource<Forum>() {
//    private var cuisineId by Delegates.notNull<Long>()
//    private var fgroup by Delegates.notNull<Long>()
//
//    fun initCuisine(id: Long, fgroup: Long) {
//        cuisineId = id
//        this.fgroup = fgroup
//    }
//
//    override suspend fun fetchData(page: Int, limit: Int): List<Forum> {
//        db.forumDao()
//    }
//
//}
