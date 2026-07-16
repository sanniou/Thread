package ai.saniou.thread

import kotlin.test.Test
import kotlin.test.assertEquals

class ReleaseServiceDescriptorTest {
    @Test
    fun releaseServiceDescriptorsContainEveryRuntimeProvider() {
        EXPECTED_PROVIDERS.forEach { (service, expected) ->
            val actual = checkNotNull(javaClass.classLoader.getResourceAsStream("META-INF/services/$service")) {
                "Missing release service descriptor for $service"
            }.bufferedReader().useLines { lines ->
                lines.map(String::trim)
                    .filter { it.isNotEmpty() && !it.startsWith('#') }
                    .toSet()
            }

            assertEquals(expected, actual, "Incomplete release service descriptor for $service")
        }
    }

    private companion object {
        val EXPECTED_PROVIDERS = mapOf(
            "io.ktor.client.HttpClientEngineContainer" to setOf(
                "io.ktor.client.engine.cio.CIOEngineContainer",
                "io.ktor.client.engine.java.JavaHttpEngineContainer",
                "io.ktor.client.engine.okhttp.OkHttpEngineContainer",
            ),
            "com.github.panpf.sketch.util.ComponentProvider" to setOf(
                "com.github.panpf.sketch.util.GifComponentProvider",
                "com.github.panpf.sketch.util.AnimatedWebpComponentProvider",
                "com.github.panpf.sketch.util.ComposeResourceComponentProvider",
                "com.github.panpf.sketch.util.KtorHttpComponentProvider",
                "com.github.panpf.sketch.util.SvgComponentProvider",
            ),
            "nl.adaptivity.xmlutil.util.SerializationProvider" to setOf(
                "nl.adaptivity.xmlutil.util.DefaultSerializationProvider",
                "nl.adaptivity.xmlutil.serialization.KotlinxSerializationProvider",
            ),
        )
    }
}
