package basic

import org.junit.Test
import testApi.testRunner.AbstractCoreTest

class DokkaBasicTests : AbstractCoreTest() {

    @Test
    fun basic1() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package basic
            |
            |class Test {
            |   val tI = 1
            |   fun tF() = 2
            |}
        """.trimMargin(),
            configuration
        ) {
            pagesGenerationStage = {
                println(it.dri)
            }
        }
    }
}