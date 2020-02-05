package multiplatform

import org.junit.Assert.assertEquals
import org.junit.Test
import testApi.testRunner.AbstractCoreTest

class BasicMultiplatformTest : AbstractCoreTest() {

    @Test
    fun dataTestExample() {
        val testDataDir = getTestDataDir("multiplatform/basicMultiplatformTest").toAbsolutePath()

        val configuration = dokkaConfiguration {
            passes {
                pass {
                    sourceRoots = listOf("$testDataDir/jvmMain/")
                }
            }
        }

        testFromData(configuration) {
            pagesTransformationStage = {
                assertEquals(6, it.children.firstOrNull()?.children?.count() ?: 0)
            }
        }
    }

    @Test
    fun inlineTestExample() {
        val configuration = dokkaConfiguration {
            passes {
                passOnPlatforms("jvm", "js") {
                    sourceRoots = listOf("src/main/kotlin/multiplatform/Test.kt")
                    targets = listOf("jvm", "js")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/multiplatform/Test.kt
            |package multiplatform
            |
            |object Test {
            |   fun test2(str: String): Unit {println(str)}
            |}
        """.trimMargin(),
            configuration
        ) {
            pagesGenerationStage = {
                println(it.dri)
                assertEquals(7, it.parentMap.size)
            }
        }
    }
}