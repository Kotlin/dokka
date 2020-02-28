package enums

import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.ModulePageNode
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest

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
                assert(test != null) { "Test not found" }
                assert(test!!.any { it.name == "E1" } && test.any { it.name == "E2" }) { "Enum entries missing in parent" }
                assert(map.keys.any { it.name == "E1" } && map.keys.any { it.name == "E2" }) { "Enum entries missing" }
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
            documentablesCreationStage = {m ->
                assertTrue("Module list cannot be empty", m.isNotEmpty())
                m.first().packages.let { p ->
                    assertTrue("Package list cannot be empty", p.isNotEmpty())
                    p.first().classlikes.let { c ->
                        assertTrue("Classlikes list cannot be empty", c.isNotEmpty())

                        val enum = c.first() as Enum
                        assertEquals(enum.name, "Test")
                        assertEquals(enum.entries.count(), 2)
                        assertNotNull(enum.companion)
                    }
                }
            }
            pagesGenerationStage = {
                val map = it.getClasslikeToMemberMap()
                val test = map.filterKeys { it.name == "Test" }.values.firstOrNull()
                assert(test != null) { "Test not found" }
                assert(test!!.any { it.name == "E1" } && test.any { it.name == "E2" }) { "Enum entries missing in parent" }
                assert(map.keys.any { it.name == "E1" } && map.keys.any { it.name == "E2" }) { "Enum entries missing" }
            }
        }
    }


    fun ModulePageNode.getClasslikeToMemberMap() =
        this.parentMap.filterValues { it is ClasslikePageNode }.entries.groupBy({ it.value }) { it.key }
}