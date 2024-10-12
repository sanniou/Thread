package ai.saniou.thread

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform