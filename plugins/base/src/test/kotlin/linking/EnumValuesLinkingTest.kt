package linking

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRIExtraContainer
import org.jetbrains.dokka.links.EnumEntryDRIExtra
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.DocumentationLink
import org.jetbrains.dokka.pages.ContentDRILink
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import utils.TestOutputWriterPlugin
import kotlin.AssertionError

class EnumValuesLinkingTest : BaseAbstractTest() {

    @Test
    fun `check if enum values are correctly linked`() {
        val writerPlugin = TestOutputWriterPlugin()
        val testDataDir = getTestDataDir("linking").toAbsolutePath()
        testFromData(
            dokkaConfiguration {
                sourceSets {
                    sourceSet {
                        sourceRoots = listOf(Paths.get("$testDataDir/jvmMain/kotlin").toString())
                        analysisPlatform = "jvm"
                        name = "jvm"
                    }
                }
            },
            pluginOverrides = listOf(writerPlugin)
        ) {
            documentablesTransformationStage = {
                val classlikes = it.packages.single().children
                assertEquals(4, classlikes.size)

                val javaLinker = classlikes.single { it.name == "JavaLinker" }
                javaLinker.documentation.values.single().children.run {
                    when (val kotlinLink = this[0].children[1].children[1]) {
                        is DocumentationLink -> kotlinLink.dri.run {
                            assertEquals("KotlinEnum.ON_CREATE", this.classNames)
                            assertEquals(null, this.callable)
                            assertNotNull(DRIExtraContainer(extra)[EnumEntryDRIExtra])
                        }
                        else -> throw AssertionError("Link node is not DocumentationLink type")
                    }

                    when (val javaLink = this[0].children[2].children[1]) {
                        is DocumentationLink -> javaLink.dri.run {
                            assertEquals("JavaEnum.ON_DECEIT", this.classNames)
                            assertEquals(null, this.callable)
                            assertNotNull(DRIExtraContainer(extra)[EnumEntryDRIExtra])
                        }
                        else -> throw AssertionError("Link node is not DocumentationLink type")
                    }
                }

                val kotlinLinker = classlikes.single { it.name == "KotlinLinker" }
                kotlinLinker.documentation.values.single().children.run {
                    when (val kotlinLink = this[0].children[0].children[5]) {
                        is DocumentationLink -> kotlinLink.dri.run {
                            assertEquals("KotlinEnum.ON_CREATE", this.classNames)
                            assertEquals(null, this.callable)
                            assertNotNull(DRIExtraContainer(extra)[EnumEntryDRIExtra])
                        }
                        else -> throw AssertionError("Link node is not DocumentationLink type")
                    }

                    when (val javaLink = this[0].children[0].children[9]) {
                        is DocumentationLink -> javaLink.dri.run {
                            assertEquals("JavaEnum.ON_DECEIT", this.classNames)
                            assertEquals(null, this.callable)
                            assertNotNull(DRIExtraContainer(extra)[EnumEntryDRIExtra])
                        }
                        else -> throw AssertionError("Link node is not DocumentationLink type")
                    }
                }

                assertEquals(
                    javaLinker.documentation.values.single().children[0].children[1].children[1].safeAs<DocumentationLink>()?.dri,
                    kotlinLinker.documentation.values.single().children[0].children[0].children[5].safeAs<DocumentationLink>()?.dri
                )

                assertEquals(
                    javaLinker.documentation.values.single().children[0].children[2].children[1].safeAs<DocumentationLink>()?.dri,
                    kotlinLinker.documentation.values.single().children[0].children[0].children[9].safeAs<DocumentationLink>()?.dri
                )
            }

            renderingStage = { rootPageNode, _ ->
                val classlikes = rootPageNode.children.single().children
                assertEquals(4, classlikes.size)

                val javaLinker = classlikes.single { it.name == "JavaLinker" }
                (javaLinker as ContentPage).run {
                    assertNotNull(content.dfs { it is ContentDRILink && it.address.classNames == "KotlinEnum.ON_CREATE" })
                    assertNotNull(content.dfs { it is ContentDRILink && it.address.classNames == "JavaEnum.ON_DECEIT" })
                }

                val kotlinLinker = classlikes.single { it.name == "KotlinLinker" }
                (kotlinLinker as ContentPage).run {
                    assertNotNull(content.dfs { it is ContentDRILink && it.address.classNames == "KotlinEnum.ON_CREATE" })
                    assertNotNull(content.dfs { it is ContentDRILink && it.address.classNames == "JavaEnum.ON_DECEIT" })
                }

                Jsoup
                    .parse(writerPlugin.writer.contents.getValue("root/linking.source/-java-linker/index.html"))
                    .select("a[href=\"../-kotlin-enum/-o-n_-c-r-e-a-t-e/index.html\"]")
                    .assertOnlyOneElement()

                Jsoup
                    .parse(writerPlugin.writer.contents.getValue("root/linking.source/-java-linker/index.html"))
                    .select("a[href=\"../-java-enum/-o-n_-d-e-c-e-i-t/index.html\"]")
                    .assertOnlyOneElement()

                Jsoup
                    .parse(writerPlugin.writer.contents.getValue("root/linking.source/-kotlin-linker/index.html"))
                    .select("a[href=\"../-kotlin-enum/-o-n_-c-r-e-a-t-e/index.html\"]")
                    .assertOnlyOneElement()

                Jsoup
                    .parse(writerPlugin.writer.contents.getValue("root/linking.source/-kotlin-linker/index.html"))
                    .select("a[href=\"../-java-enum/-o-n_-d-e-c-e-i-t/index.html\"]")
                    .assertOnlyOneElement()
            }
        }
    }

    private fun <T> List<T>.assertOnlyOneElement() {
        if (isEmpty() || size > 1) {
            throw AssertionError("Single element expected in list: $this")
        }
    }
}
