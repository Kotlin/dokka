package locationProvider

import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URL

class MultiModuleLinkingTest : BaseAbstractTest() {
    private val testDataDir =
        getTestDataDir("locationProvider").toAbsolutePath().toString().removePrefix("/").let { "/$it" }
    private val exampleDomain = "https://example.com"
    private val packageListURL = URL("file://$testDataDir/multi-module-package-list")
    private val kotlinLang = "https://kotlinlang.org/api/latest/jvm/stdlib"
    private val stdlibPackageListURL = URL("file://$testDataDir/stdlib-package-list")
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath += jvmStdlibPath!!
            }
        }
    }

    private fun getTestLocationProvider(context: DokkaContext? = null): DefaultExternalLocationProvider {
        val dokkaContext = context ?: DokkaContext.create(configuration, logger, emptyList())
        val packageList = PackageList.load(packageListURL, 8, true)!!
        val externalDocumentation =
            ExternalDocumentation(URL(exampleDomain), packageList)
        return DefaultExternalLocationProvider(externalDocumentation, ".html", dokkaContext)
    }

    private fun getStdlibTestLocationProvider(context: DokkaContext? = null): DefaultExternalLocationProvider {
        val dokkaContext = context ?: DokkaContext.create(configuration, logger, emptyList())
        val packageList = PackageList.load(stdlibPackageListURL, 8, true)!!
        val externalDocumentation =
                ExternalDocumentation(URL(kotlinLang), packageList)
        return DefaultExternalLocationProvider(externalDocumentation, ".html", dokkaContext)
    }

    @Test
    fun `should link to a multi-module declaration`() {
        val locationProvider = getTestLocationProvider()
        val dri = DRI("baz", "BazClass")

        assertEquals("$exampleDomain/moduleB/baz/-baz-class/index.html", locationProvider.resolve(dri))
    }

    @Test
    fun `should not fail on non-present package`() {
        val stdlibLocationProvider = getStdlibTestLocationProvider()
        val locationProvider = getTestLocationProvider()
        val dri = DRI("baz", "BazClass")

        assertEquals(null, stdlibLocationProvider.resolve(dri))
        assertEquals("$exampleDomain/moduleB/baz/-baz-class/index.html", locationProvider.resolve(dri))
    }

    @Test
    fun `should handle relocations`() {
        val locationProvider = getTestLocationProvider()
        val dri = DRI("", "NoPackageClass")

        assertEquals("$exampleDomain/moduleB/[root]/-no-package-class/index.html", locationProvider.resolve(dri))
    }
}
