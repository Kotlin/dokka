import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.templating.toJsonString
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.sitemap.SitemapConfiguration
import org.jetbrains.dokka.sitemap.SitemapPlugin
import org.junit.jupiter.api.Test
import utils.TestOutputWriterPlugin
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SitemapTest: BaseAbstractTest() {
    private val testDataDir = getTestDataDir("basicProject").toAbsolutePath()

    private val configuration = dokkaConfiguration {
        moduleName = "example"
        delayTemplateSubstitution = false
        sourceSets {
            val jvm = sourceSet {
                name = "jvm"
                displayName = "jvm"
                analysisPlatform = "jvm"
                sourceRoots = listOf(Paths.get("$testDataDir/basic").toString())
            }
        }
    }

    private val expectedEntriesWithoutBaseUrl = listOf(
        "index.html",
        "example/basicProject/index.html",
        "example/basicProject/-main/index.html",
        "example/basicProject/-main/-main.html",
        "example/basicProject/-main/main-fun.html",
        "example/basicProject/-main/main-value.html"
    )


    @Test
    fun `sitemaps should be generated even without any configuration`(){
        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin, SitemapPlugin())
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.writer.contents[SitemapConfiguration.defaultRelativeOutputLocation]
                assertNotNull(content, "Expected a sitemap")
                assertEquals(expectedEntriesWithoutBaseUrl.joinToString(separator = "\n"), content)
            }
        }
    }

    @Test
    fun `sitemap should include base url`(){
        val sitemapConfiguration = SitemapConfiguration(baseUrl = "www.kotlin.com")
        val pluginConf = PluginConfigurationImpl(
            fqPluginName = SitemapPlugin::class.qualifiedName!!,
            serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
            values = toJsonString(sitemapConfiguration)
        )

        val writerPlugin = TestOutputWriterPlugin()
        testFromData(
            configuration.copy(pluginsConfiguration = listOf(pluginConf)),
            pluginOverrides = listOf(writerPlugin, SitemapPlugin())
        ) {
            renderingStage = { _, _ ->
                val expected = expectedEntriesWithoutBaseUrl.map { "${sitemapConfiguration.baseUrl}/$it" }.joinToString(separator = "\n")

                val content = writerPlugin.writer.contents[SitemapConfiguration.defaultRelativeOutputLocation]
                assertNotNull(content, "Expected a sitemap")
                assertEquals(expected, content)
            }
        }
    }

    @Test
    fun `sitemap should be placed in configured location`(){
        val sitemapConfiguration = SitemapConfiguration(relativeOutputLocation = "sitemaps/sample/example.txt")
        val pluginConf = PluginConfigurationImpl(
            fqPluginName = SitemapPlugin::class.qualifiedName!!,
            serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
            values = toJsonString(sitemapConfiguration)
        )

        val writerPlugin = TestOutputWriterPlugin()
        testFromData(
            configuration.copy(pluginsConfiguration = listOf(pluginConf)),
            pluginOverrides = listOf(writerPlugin, SitemapPlugin())
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.writer.contents[sitemapConfiguration.relativeOutputLocation]
                assertNotNull(content, "Expected a sitemap")
            }
        }
    }

    @Test
    fun `warning should be displayed when no configuration for sitemap is provided`(){
        val writerPlugin = TestOutputWriterPlugin()
        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin, SitemapPlugin())
        ) {
            renderingStage = { _, _ ->
                val expectedMessage = "Failed to find configured value for baseUrl. Sitemap plugin will generate only relative paths from root page, " +
                        "that need to be appended manually to site's url"
                val message = logger.warnMessages.single()
                assertEquals(expectedMessage, message)
            }
        }
    }
}