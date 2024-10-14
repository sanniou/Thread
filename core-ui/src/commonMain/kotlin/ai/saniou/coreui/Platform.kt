package ai.saniou.coreui

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
