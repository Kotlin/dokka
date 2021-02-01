package kotlinAsJavaPlugin

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JvmOverloadsTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath += jvmStdlibPath!!
            }
        }
    }

    @Test
    fun `should generate multiple functions`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |@JvmOverloads
            |fun sample(a: Int = 0, b: String, c: Int = 0): String = ""
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val functions = module.packages.flatMap { it.classlikes }.flatMap { it.functions }
                assertEquals(3, functions.size)
                assertEquals(3, functions[0].parameters.size)
                assertEquals(2, functions[1].parameters.size)
                assertEquals(1, functions[2].parameters.size)
            }
        }
    }

    @Test
    fun `should do nothing if there is no default values`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |@JvmOverloads
            |fun sample(a: Int, b: String, c: Int): String = ""
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val functions = module.packages.flatMap { it.classlikes }.flatMap { it.functions }
                assertEquals(1, functions.size)
                assertEquals(3, functions[0].parameters.size)
            }
        }
    }
}