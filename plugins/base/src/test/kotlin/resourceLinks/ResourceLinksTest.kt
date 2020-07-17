package resourceLinks

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import utils.TestOutputWriterPlugin

class ResourceLinksTest : AbstractCoreTest() {
    class TestResourcesAppenderPlugin(val resources: List<String>) : DokkaPlugin() {
        class TestResourcesAppender(val resources: List<String>) : PageTransformer {
            override fun invoke(input: RootPageNode) = input.transformContentPagesTree {
                    it.modified(
                        embeddedResources = it.embeddedResources + resources
                    )
                }
        }

        val appender by extending {
            plugin<DokkaBase>().htmlPreprocessors with TestResourcesAppender(resources)
        }
    }
    @Test
    fun resourceLinksTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }
        val absoluteResources = listOf(
            "https://stackpath.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css",
            "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.5.1/jquery.min.js"
        )
        val relativeResources = listOf(
            "test/relativePath.js",
            "test/relativePath.css"
        )

        val source =
            """
            |/src/main/kotlin/test/Test.kt
            |package example
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(TestResourcesAppenderPlugin(absoluteResources + relativeResources), writerPlugin)
        ) {
            renderingStage = {
                root, context -> Jsoup
                .parse(writerPlugin.writer.contents["root/example.html"])
                .head()
                .select("link, script")
                .let {
                    absoluteResources.forEach {
                        r -> assert(it.`is`("[href=$r], [src=$r]"))
                    }
                    relativeResources.forEach {
                        r -> assert(it.`is`("[href=../$r] , [src=../$r]"))
                    }
                }
            }
        }
    }
}
