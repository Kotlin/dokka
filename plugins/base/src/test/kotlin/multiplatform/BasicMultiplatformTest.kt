package multiplatform

import org.junit.Assert.assertEquals
import org.junit.Test
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest

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
                pass {
                    sourceRoots = listOf("src/main/kotlin/multiplatform/Test.kt")
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
                assertEquals(6, it.parentMap.size)
            }
        }
    }
}