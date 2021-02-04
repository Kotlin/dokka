package kotlinAsJavaPlugin

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JvmSyntheticTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath += jvmStdlibPath!!
            }
        }
    }

    @Test
    fun `should not include synthetic functions`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |@JvmSynthetic
            |fun synthetic(): String = ""
            |fun sample(): String = ""
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val functions = module.packages.flatMap { it.classlikes }.flatMap { it.functions }
                assertEquals(1, functions.size)
                assertEquals("sample", functions[0].name)
            }
        }
    }

    @Test
    fun `should check synthetic on method fields, getters and setters`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |@get:JvmSynthetic
            |var synthetic: String = ""
            |
            |@set:JvmSynthetic
            |var synthetic2: String = ""
            |
            |@JvmSynthetic
            |var synthetic3: String = ""
            |
            |var sample: String = ""
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val functions = module.packages.flatMap { it.classlikes }.flatMap { it.functions }
                assertEquals(4, functions.size)
                assertEquals("setSynthetic", functions[0].name)
                assertEquals("getSynthetic2", functions[1].name)
                assertEquals("getSample", functions[2].name)
                assertEquals("setSample", functions[3].name)
            }
        }
    }
}