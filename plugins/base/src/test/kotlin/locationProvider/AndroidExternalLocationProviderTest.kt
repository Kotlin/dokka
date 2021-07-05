package locationProvider

import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.external.javadoc.AndroidExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URL

class AndroidExternalLocationProviderTest : BaseAbstractTest() {
    private val android = ExternalDocumentation(
        URL("https://developer.android.com/reference/kotlin"),
        PackageList(
            RecognizedLinkFormat.DokkaHtml,
            mapOf("" to setOf("android.content", "android.net")),
            emptyMap(),
            URL("file://not-used")
        )
    )
    private val androidx = ExternalDocumentation(
        URL("https://developer.android.com/reference/kotlin"),
        PackageList(
            RecognizedLinkFormat.DokkaHtml,
            mapOf("" to setOf("androidx.appcompat.app")),
            emptyMap(),
            URL("file://not-used")
        )
    )
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath += jvmStdlibPath!!
            }
        }
    }

    private fun getTestLocationProvider(
        externalDocumentation: ExternalDocumentation,
        context: DokkaContext? = null
    ): DefaultExternalLocationProvider {
        val dokkaContext = context ?: DokkaContext.create(configuration, logger, emptyList())
        return AndroidExternalLocationProvider(externalDocumentation, dokkaContext)
    }

    @Test
    fun `#1230 anchor to a method from AndroidX`() {
        val locationProvider = getTestLocationProvider(androidx)
        val dri = DRI(
            "androidx.appcompat.app",
            "AppCompatActivity",
            Callable("findViewById", null, listOf(TypeConstructor("kotlin.Int", emptyList())))
        )

        assertEquals(
            "${androidx.documentationURL}/androidx/appcompat/app/AppCompatActivity.html#findviewbyid",
            locationProvider.resolve(dri)
        )
    }

    @Test
    fun `anchor to a method from Android`() {
        val locationProvider = getTestLocationProvider(android)
        val dri = DRI(
            "android.content",
            "ContextWrapper",
            Callable(
                "checkCallingUriPermission",
                null,
                listOf(
                    TypeConstructor("android.net.Uri", emptyList()),
                    TypeConstructor("kotlin.Int", emptyList())
                )
            )
        )

        assertEquals(
            "${android.documentationURL}/android/content/ContextWrapper.html#checkcallinguripermission",
            locationProvider.resolve(dri)
        )
    }

    @Test
    fun `should return null for method not in list`() {
        val locationProvider = getTestLocationProvider(android)
        val dri = DRI(
            "foo",
            "Bar",
            Callable(
                "baz",
                null,
                emptyList()
            )
        )

        assertEquals(null, locationProvider.resolve(dri))
    }
}
