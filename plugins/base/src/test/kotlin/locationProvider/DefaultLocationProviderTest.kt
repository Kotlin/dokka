package locationProvider

import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.base.resolvers.local.DefaultLocationProvider
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class DefaultLocationProviderTest: AbstractCoreTest() {
    @Test
    fun `#644 same directory for module and package`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

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
                val lp = DefaultLocationProvider(module, context!!)
                assertNotEquals(lp.resolve(module.children.single()).removePrefix("/"), lp.resolve(module))
            }
        }
    }
}
