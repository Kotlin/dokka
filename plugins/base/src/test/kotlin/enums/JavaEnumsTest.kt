package enums

import org.jetbrains.dokka.SourceLinkDefinitionImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.ObviousMember
import org.junit.jupiter.api.Test
import signatures.renderedContent
import utils.TestOutputWriterPlugin
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JavaEnumsTest : BaseAbstractTest() {

    private val basicConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
    }

    // Shouldn't try to give source links to synthetic methods (values, valueOf) if any are present
    // https://github.com/Kotlin/dokka/issues/2544
    @Test
    fun `java enum with configured source links should not fail build due to any synthetic methods`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    sourceLinks = listOf(
                        SourceLinkDefinitionImpl(
                            localDirectory = "src/main/java",
                            remoteUrl = URL("https://github.com/user/repo/tree/master/src/main/java"),
                            remoteLineSuffix = "#L"
                        )
                    )
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/java/basic/JavaEnum.java
            |package testpackage
            |
            |public enum JavaEnum {
            |    ONE, TWO, THREE
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val enumPage = writerPlugin.writer.renderedContent("root/testpackage/-java-enum/index.html")
                val sourceLink = enumPage.select("div[data-togglable=Sources]")
                    .select("a[href]")
                    .attr("href")


                assertEquals(
                    "https://github.com/user/repo/tree/master/src/main/java/basic/JavaEnum.java#L3",
                    sourceLink
                )
            }
        }
    }

    @Test
    fun `should mark synthetic functions generated for Kotlin as obvious`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/java/basic/JavaEnum.java
            |package testpackage
            |
            |public enum JavaEnum {
            |    ONE, TWO
            |}
        """.trimMargin(),
            basicConfiguration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            documentablesCreationStage = { modules ->
                val pckg = modules.flatMap { it.packages }.single { it.packageName == "testpackage" }
                val enum = pckg.children.single { it is DEnum } as DEnum

                // there's two with the same name, one inherited from
                // java.lang.Enum and one is synthetic for Kotlin interop
                enum.functions.filter { it.name == "valueOf" }.let { valueOfMethods ->
                    assertEquals(2, valueOfMethods.size)

                    val valueOfFromKotlin = valueOfMethods[0]
                    assertEquals(
                        "testpackage/JavaEnum/valueOf/#java.lang.String/PointingToDeclaration/",
                        valueOfFromKotlin.dri.toString()
                    )
                    assertNotNull(valueOfFromKotlin.extra[ObviousMember])

                    val valueOfFromJava = valueOfMethods[1]
                    assertEquals(
                        "java.lang/Enum/valueOf/#java.lang.Class<T>#java.lang.String/PointingToDeclaration/",
                        valueOfFromJava.dri.toString()
                    )
                    assertNotNull(valueOfFromJava.extra[ObviousMember])
                }

                val valuesMethod = enum.functions.single { it.name == "values" }
                assertEquals("testpackage/JavaEnum/values/#/PointingToDeclaration/", valuesMethod.dri.toString())
                assertNotNull(valuesMethod.extra[ObviousMember])
            }
        }
    }
}
