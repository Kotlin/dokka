package basic

import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.ModulePageNode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import kotlin.test.assertEquals

class DokkaBasicTests : BaseAbstractTest() {

    @Test
    fun basic1() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
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
                val root = it as ModulePageNode
                assertEquals(3, root.getClasslikeToMemberMap().filterKeys { it.name == "Test" }.entries.firstOrNull()?.value?.size)
            }
        }
    }

    private fun ModulePageNode.getClasslikeToMemberMap() =
        this.parentMap.filterValues { it is ClasslikePageNode }.entries.groupBy({ it.value }) { it.key }
}
