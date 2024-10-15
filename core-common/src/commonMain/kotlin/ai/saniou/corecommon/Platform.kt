package ai.saniou.corecommon

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
