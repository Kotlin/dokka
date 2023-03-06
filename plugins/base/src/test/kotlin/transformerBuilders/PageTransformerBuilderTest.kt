package transformerBuilders

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.transformers.pages.pageMapper
import org.jetbrains.dokka.transformers.pages.pageScanner
import org.jetbrains.dokka.transformers.pages.pageStructureTransformer
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import utils.TestOutputWriterPlugin
import utils.assertContains
import utils.assertNotNull
class PageTransformerBuilderTest : BaseAbstractTest() {

    class ProxyPlugin(transformer: PageTransformer) : DokkaPlugin() {
        val pageTransformer by extending { CoreExtensions.pageTransformer with transformer }

        @OptIn(DokkaPluginApiPreview::class)
        override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
            PluginApiPreviewAcknowledgement
    }

    @Test
    fun scannerTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/transformerBuilder/Test.kt")
                }
            }
        }
        val list = mutableListOf<String>()

        var orig: PageNode? = null

        testInline(
            """
            |/src/main/kotlin/transformerBuilder/Test.kt
            |package transformerBuilder
            |
            |object Test {
            |   fun test2(str: String): Unit {println(str)}
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(ProxyPlugin(pageScanner {
                list += name
            }))
        ) {
            pagesGenerationStage = {
                orig = it
            }
            pagesTransformationStage = { root ->
                list.assertCount(4, "Page list: ")
                orig?.let { root.assertTransform(it) }
            }
        }
    }

    @Test
    fun mapperTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/transformerBuilder/Test.kt")
                }
            }
        }

        var orig: PageNode? = null

        testInline(
            """
            |/src/main/kotlin/transformerBuilder/Test.kt
            |package transformerBuilder
            |
            |object Test {
            |   fun test2(str: String): Unit {println(str)}
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(ProxyPlugin(pageMapper {
                modified(name = name + "2")
            }))
        ) {
            pagesGenerationStage = {
                orig = it
            }
            pagesTransformationStage = {
                it.let { root ->
                    root.name.assertEqual("root2", "Root name: ")
                    orig?.let {
                        root.assertTransform(it) { node -> node.modified(name = node.name + "2") }
                    }
                }
            }
        }
    }

    @Test
    fun structureTransformerTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/transformerBuilder/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/transformerBuilder/Test.kt
            |package transformerBuilder
            |
            |object Test {
            |   fun test2(str: String): Unit {println(str)}
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(ProxyPlugin(pageStructureTransformer {
                val ch = children.first()
                modified(
                    children = listOf(
                        ch,
                        RendererSpecificResourcePage("test", emptyList(), RenderingStrategy.DoNothing)
                    )
                )
            }))
        ) {
            pagesTransformationStage = { root ->
                root.children.assertCount(2, "Root children: ")
                root.children.first().name.assertEqual("transformerBuilder")
                root.children[1].name.assertEqual("test")
            }
        }
    }

    @Test
    fun `kotlin constructors tab should exist even though there is primary constructor only`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |class Test(val xd: Int)
        """.trimMargin(),
            configuration
        ) {
            pagesGenerationStage = { root ->
                val content = root.children
                    .flatMap { it.children<ContentPage>() }
                    .map { it.content }.single().children
                    .filterIsInstance<ContentGroup>()
                    .single { it.dci.kind == ContentKind.Main }.children

                val contentWithConstructorsHeader = content.find { tabContent -> tabContent.dfs {  it is ContentText && (it as? ContentText)?.text == "Constructors"} != null }

                contentWithConstructorsHeader.assertNotNull("contentWithConstructorsHeader")

                contentWithConstructorsHeader?.dfs { it.dci.kind == ContentKind.Constructors && it is ContentGroup }
                    .assertNotNull("constructor group")
            }
        }
    }

    @Test
    fun `should load script as defer if name ending in _deferred`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
        }
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/test/Test.kt
            |package test
            |
            |class Test
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val generatedFiles = writerPlugin.writer.contents

                assertContains(generatedFiles.keys, "scripts/symbol-parameters-wrapper_deferred.js")

                val scripts = generatedFiles.getValue("root/test/-test/-test.html").let { Jsoup.parse(it) }.select("script")
                val deferredScriptSources = scripts.filter { element -> element.hasAttr("defer") }.map { it.attr("src") }

                // important to check symbol-parameters-wrapper_deferred specifically since it might break some features
                assertContains(deferredScriptSources, "../../../scripts/symbol-parameters-wrapper_deferred.js")
            }
        }
    }

    private fun <T> Collection<T>.assertCount(n: Int, prefix: String = "") =
        assert(count() == n) { "${prefix}Expected $n, got ${count()}" }

    private fun <T> T.assertEqual(expected: T, prefix: String = "") = assert(this == expected) {
        "${prefix}Expected $expected, got $this"
    }

    private fun PageNode.assertTransform(expected: PageNode, block: (PageNode) -> PageNode = { it }): Unit = this.let {
        it.name.assertEqual(block(expected).name)
        it.children.zip(expected.children).forEach { (g, e) ->
            g.name.assertEqual(block(e).name)
            g.assertTransform(e, block)
        }
    }
}
