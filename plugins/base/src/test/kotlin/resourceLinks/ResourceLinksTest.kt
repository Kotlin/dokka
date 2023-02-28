package resourceLinks

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.base.renderers.html.TEMPLATE_REPLACEMENT
import org.jetbrains.dokka.base.templating.toJsonString
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import utils.TestOutputWriterPlugin
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ResourceLinksTest : BaseAbstractTest() {
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

        @OptIn(DokkaPluginApiPreview::class)
        override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
            PluginApiPreviewAcknowledgement
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
            renderingStage = { _, _ ->
                Jsoup
                    .parse(writerPlugin.writer.contents.getValue("root/example.html"))
                    .head()
                    .select("link, script")
                    .let {
                        absoluteResources.forEach { r ->
                            assert(it.`is`("[href=$r], [src=$r]"))
                        }
                        relativeResources.forEach { r ->
                            assert(it.`is`("[href=../$r] , [src=../$r]"))
                        }
                    }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun resourceCustomPreprocessorTest(isMultiModule: Boolean) {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
            delayTemplateSubstitution = isMultiModule
            pluginsConfigurations = mutableListOf(
                PluginConfigurationImpl(
                    DokkaBase::class.java.canonicalName,
                    DokkaConfiguration.SerializationFormat.JSON,
                    toJsonString(
                        DokkaBaseConfiguration(
                            customStyleSheets = listOf(File("test/customStyle.css")),
                            customAssets = listOf(File("test/customImage.svg"))
                        )
                    )
                )
            )
        }
        val source =
            """
            |/src/main/kotlin/test/Test.kt
            |package example
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                run {
                    if (isMultiModule) {
                        assertNull(writerPlugin.writer.contents["images/customImage.svg"])
                        assertNull(writerPlugin.writer.contents["styles/customStyle.css"])
                    } else {
                        assertNotNull(writerPlugin.writer.contents["images/customImage.svg"])
                        assertNotNull(writerPlugin.writer.contents["styles/customStyle.css"])
                    }
                    if (isMultiModule) {
                        Jsoup
                            .parse(writerPlugin.writer.contents.getValue("example.html"))
                            .head()
                            .select("link, script")
                            .let {
                                listOf("styles/customStyle.css").forEach { r ->
                                    assert(it.`is`("[href=$TEMPLATE_REPLACEMENT$r]"))
                                }
                            }
                    } else {
                        Jsoup
                            .parse(writerPlugin.writer.contents.getValue("root/example.html"))
                            .head()
                            .select("link, script")
                            .let {
                                listOf("styles/customStyle.css").forEach { r ->
                                    assert(it.`is`("[href=../$r], [src=../$r]"))
                                }
                            }
                    }
                }
            }
        }
    }

    @Test
    fun resourceMultiModuleLinksTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
            delayTemplateSubstitution = false
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
            renderingStage = { _, _ ->
                run {
                    assertNull(writerPlugin.writer.contents["scripts/relativePath.js"])
                    assertNull(writerPlugin.writer.contents["styles/relativePath.js"])
                    Jsoup
                        .parse(writerPlugin.writer.contents.getValue("root/example.html"))
                        .head()
                        .select("link, script")
                        .let {
                            absoluteResources.forEach { r ->
                                assert(it.`is`("[href=$r], [src=$r]"))
                            }
                            relativeResources.forEach { r ->
                                assert(it.`is`("[href=../$r] , [src=../$r]"))
                            }
                        }
                }
            }
        }
    }
}
