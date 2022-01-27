package locationProvider

import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.external.javadoc.JavadocExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URL

class JavadocExternalLocationProviderTest : BaseAbstractTest() {
    private val testDataDir =
        getTestDataDir("locationProvider").toAbsolutePath().toString().removePrefix("/").let { "/$it" }

    private val jdk = "https://docs.oracle.com/javase/8/docs/api/"
    private val jdkPackageListURL = URL("file://$testDataDir/jdk8-package-list")

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
        val packageList = PackageList.load(jdkPackageListURL, 8, true)!!
        val externalDocumentation =
            ExternalDocumentation(URL(jdk), packageList)
        return JavadocExternalLocationProvider(externalDocumentation, "--", "-", dokkaContext)
    }

    @Test
    fun `link to enum entity of javadoc`() {
        val locationProvider = getTestLocationProvider()
        val ktDri = DRI(
            "java.nio.file",
            "StandardOpenOption.CREATE",
            extra = DRIExtraContainer().also { it[EnumEntryDRIExtra] = EnumEntryDRIExtra }.encode()
        )
        val javaDri = DRI(
            "java.nio.file",
            "StandardOpenOption.CREATE",
            null,
            PointingToDeclaration,
            DRIExtraContainer().also { it[EnumEntryDRIExtra] = EnumEntryDRIExtra }.encode()
        )

        assertEquals(
            "https://docs.oracle.com/javase/8/docs/api/java/nio/file/StandardOpenOption.html#CREATE",
            locationProvider.resolve(ktDri)
        )

        assertEquals(
            "https://docs.oracle.com/javase/8/docs/api/java/nio/file/StandardOpenOption.html#CREATE",
            locationProvider.resolve(javaDri)
        )
    }

    @Test
    fun `link to nested class of javadoc`() {
        val locationProvider = getTestLocationProvider()
        val dri = DRI(
            "java.rmi.activation",
            "ActivationGroupDesc.CommandEnvironment"
        )

        assertEquals(
            "https://docs.oracle.com/javase/8/docs/api/java/rmi/activation/ActivationGroupDesc.CommandEnvironment.html",
            locationProvider.resolve(dri)
        )
    }
}
