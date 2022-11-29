package content.samples

import matchers.content.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Test
import utils.classSignature
import utils.findTestType
import java.nio.file.Paths

class ContentForSamplesTest : BaseAbstractTest() {
    private val testDataDir = getTestDataDir("content/samples").toAbsolutePath()

    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
                samples = listOf(
                    Paths.get("$testDataDir/samples.kt").toString(),
                )
            }
        }
    }

    @Test
    fun `samples block is rendered in the description`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            | /**
            | * @sample [test.sampleForClassDescription]
            | */
            |class Foo
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "Foo")
                page.content.assertNode {
                    group {
                        header(1) { +"Foo" }
                        platformHinted {
                            classSignature(
                                emptyMap(),
                                "",
                                "",
                                emptySet(),
                                "Foo"
                            )
                            header(4) { +"Samples" }
                            group {
                                codeBlock {
                                    +"""|
                                    |fun main() { 
                                    |   //sampleStart 
                                    |   print("Hello") 
                                    |   //sampleEnd
                                    |}""".trimMargin()
                                }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }
}