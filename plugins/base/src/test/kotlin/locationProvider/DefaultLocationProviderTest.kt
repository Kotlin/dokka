package locationProvider

import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DefaultLocationProviderTest : AbstractCoreTest() {
    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
    }

    @Test
    fun `#644 same directory for module and package`() {
        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |
            |class Test {
            |   val x = 1
            |}
        """.trimMargin(),
            configuration
        ) {
            var context: DokkaContext? = null
            pluginsSetupStage = {
                context = it
            }

            pagesGenerationStage = { module ->
                val lp = DokkaLocationProvider(module, context!!)
                assertNotEquals(lp.resolve(module.children.single()).removePrefix("/"), lp.resolve(module))
            }
        }
    }

    @Test
    fun `should escape illegal pipe character in file name`() {
        /*
        Currently even kotlin doesn't escape pipe characters in file names so it is impossible to have a
        class named || on windows
         */
        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |
            |class Test {
            |   fun `||`() { }
            |}
        """.trimMargin(),
            configuration
        ) {
            var context: DokkaContext? = null
            pluginsSetupStage = {
                context = it
            }

            pagesGenerationStage = { module ->
                val lp = DokkaLocationProvider(module, context!!)
                val functionWithPipes = module.dfs { it.name == "||" }
                assertNotNull(functionWithPipes, "Failed to find a page for a function named ||")
                assertEquals(lp.resolve(functionWithPipes), "[root]/-test/[124][124].html")
            }
        }
    }

    @ParameterizedTest
    @MethodSource
    fun runEscapeTestForCharacter(data: TestData) {
        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |
            |class Test {
            |   fun `${data.tested}`() { }
            |}
        """.trimMargin(),
            configuration
        ) {
            var context: DokkaContext? = null
            pluginsSetupStage = {
                context = it
            }

            pagesGenerationStage = { module ->
                val lp = DokkaLocationProvider(module, context!!)
                val functionWithPipes = module.dfs { it.name == "${data.tested}" }
                assertNotNull(functionWithPipes, "Failed to find a page for a function named ${data.tested}")
                assertEquals(lp.resolve(functionWithPipes), "[root]/-test/${data.expectedReplacement}.html")
            }
        }
    }

    data class TestData(val tested: Char, val expectedReplacement: String)

    companion object TestDataSources {
        @JvmStatic
        fun runEscapeTestForCharacter(): List<TestData> = listOf(
            '|' to "[124]",
            '>' to "[62]",
            '<' to "[60]",
            '*' to "[42]",
            ':' to "[58]",
            '"' to "[34]",
            '?' to "[63]",
            '%' to "[37]"
        ).map {
            TestData(it.first, it.second)
        }
    }
}
