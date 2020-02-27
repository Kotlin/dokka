package basic

import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.ModulePageNode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest

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
                assertTrue(it.getClasslikeToMemberMap().filterKeys { it.name == "Test" }.entries.firstOrNull()?.value?.size == 5)
            }
        }
    }

    private fun ModulePageNode.getClasslikeToMemberMap() =
        this.parentMap.filterValues { it is ClasslikePageNode }.entries.groupBy({ it.value }) { it.key }
}