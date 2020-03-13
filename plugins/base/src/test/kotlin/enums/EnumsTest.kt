package enums

import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.junit.jupiter.api.Test
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Assertions.*

class EnumsTest : AbstractCoreTest() {

    @Test
    fun basicEnum() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package enums
            |
            |enum class Test {
            |   E1,
            |   E2
            |}
        """.trimMargin(),
            configuration
        ) {
            pagesGenerationStage = {
                val map = it.getClasslikeToMemberMap()
                val test = map.filterKeys { it.name == "Test" }.values.firstOrNull()
                assertTrue(test != null) { "Test not found" }
                assertTrue(test!!.any { it.name == "E1" } && test.any { it.name == "E2" }) { "Enum entries missing in parent" }
            }
        }
    }

    @Test
    fun enumWithCompanion() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package enums
            |
            |enum class Test {
            |   E1,
            |   E2;
            |   companion object {}
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesTransformationStage = { m ->
                m.packages.let { p ->
                    assertTrue(p.isNotEmpty(), "Package list cannot be empty")
                    p.first().classlikes.let { c ->
                        assertTrue(c.isNotEmpty(), "Classlikes list cannot be empty")

                        val enum = c.first() as DEnum
                        assertEquals(enum.name, "Test")
                        assertEquals(enum.entries.count(), 2)
                        assertNotNull(enum.companion)
                    }
                }
            }
            pagesGenerationStage = { module ->
                val map = module.getClasslikeToMemberMap()
                val test = map.filterKeys { it.name == "Test" }.values.firstOrNull()
                assertNotNull(test, "Test not found")
                assertTrue(test!!.any { it.name == "E1" } && test.any { it.name == "E2" }) { "Enum entries missing in parent" }
            }
        }
    }


    fun RootPageNode.getClasslikeToMemberMap() =
        this.parentMap.filterValues { it is ClasslikePageNode }.entries.groupBy({ it.value }) { it.key }
}