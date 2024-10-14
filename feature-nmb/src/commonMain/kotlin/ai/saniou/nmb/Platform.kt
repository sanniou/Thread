package ai.saniou.nmb

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
