package locationProvider

import org.jetbrains.dokka.base.resolvers.external.Dokka010ExternalLocationProvider
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URL

class Dokka010ExternalLocationProviderTest : AbstractCoreTest() {
    private val testDataDir = getTestDataDir("locationProvider").toAbsolutePath()
    private val kotlinLang = "https://kotlinlang.org/api/latest/jvm/stdlib"
    private val packageListURL = URL("file://$testDataDir/old-package-list")
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath += jvmStdlibPath!!
                externalDocumentationLink(kotlinLang, packageListURL.toString())
            }
        }
    }

    private fun getTestLocationProvider(context: DokkaContext? = null): Dokka010ExternalLocationProvider {
        val dokkaContext = context ?: DokkaContext.create(configuration, logger, emptyList())
        val packageList = PackageList.load(packageListURL, 8, true)!!
        val externalDocumentation =
            ExternalDocumentation(URL(kotlinLang), packageList)
        return Dokka010ExternalLocationProvider(externalDocumentation, ".html", dokkaContext)
    }

    @Test
    fun `ordinary link`() {
        val locationProvider = getTestLocationProvider()
        val dri = DRI("kotlin.reflect", "KVisibility")

        assertEquals("$kotlinLang/kotlin.reflect/-k-visibility/index.html", locationProvider.resolve(dri))
    }

    @Test
    fun `relocation in package list`() {
        val locationProvider = getTestLocationProvider()
        val dri = DRI("kotlin.text", "StringBuilder")

        assertEquals("$kotlinLang/kotlin.relocated.text/-string-builder/index.html", locationProvider.resolve(dri))
    }

    @Test
    fun `method relocation in package list`() {
        val locationProvider = getTestLocationProvider()
        val dri = DRI(
            "kotlin",
            "",
            Callable(
                "minus",
                null,
                listOf(
                    TypeConstructor("java.math.BigDecimal", emptyList()),
                    TypeConstructor("java.math.BigDecimal", emptyList())
                )
            )
        )

        assertEquals("$kotlinLang/kotlin/java.math.-big-decimal/minus.html", locationProvider.resolve(dri))
    }
}
