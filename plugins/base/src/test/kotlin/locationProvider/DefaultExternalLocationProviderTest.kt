package locationProvider

import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URL

class DefaultExternalLocationProviderTest : AbstractCoreTest() {
    private val testDataDir =
        getTestDataDir("locationProvider").toAbsolutePath().toString().removePrefix("/").let { "/$it" }
    private val kotlinLang = "https://kotlinlang.org/api/latest/jvm/stdlib"
    private val packageListURL = URL("file://$testDataDir/stdlib-package-list")
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath += jvmStdlibPath!!
                ExternalDocumentationLink(kotlinLang, packageListURL.toString())
            }
        }
    }

    private fun getTestLocationProvider(context: DokkaContext? = null): DefaultExternalLocationProvider {
        val dokkaContext = context ?: DokkaContext.create(configuration, logger, emptyList())
        val packageList = PackageList.load(packageListURL, 8, true)!!
        val externalDocumentation =
            ExternalDocumentation(URL(kotlinLang), packageList)
        return DefaultExternalLocationProvider(externalDocumentation, ".html", dokkaContext)
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
        val dri = DRI(
            "",
            "",
            Callable(
                "longArray",
                null,
                listOf(
                    TypeConstructor("kotlin.Int", emptyList()),
                    TypeConstructor("kotlin.Any", emptyList())
                )
            )
        )

        assertEquals("$kotlinLang/kotlin-stdlib/[JS root]/long-array.html", locationProvider.resolve(dri))
    }
}
