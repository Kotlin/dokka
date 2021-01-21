package kotlinAsJavaPlugin


import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DEnum
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DRITranslationTest : BaseAbstractTest() {
    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath += jvmStdlibPath!!
            }
        }
    }

    @Test
    fun `should correctly handle nested classes`() {
        testInline(
                """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class A {
            |   class B(val x: String)
            |}
            |class C {
            |   class B(val x: String)
            |}
        """.trimMargin(),
                configuration,
        ) {
            documentablesTransformationStage = { module ->
                val nestedClasslikesDRIs = module.packages.flatMap { it.classlikes }.flatMap { it.classlikes }.map { it.dri }
                val driRegex = "[AC]\\.B".toRegex()

                nestedClasslikesDRIs.forEach { dri ->
                    assertTrue(driRegex.matches(dri.classNames.toString()))
                }
            }
        }
    }

    @Test
    fun `should correctly handle interface methods`() {
        testInline(
                """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |interface A {
            |   fun b()
            |}
        """.trimMargin(),
                configuration,
        ) {
            documentablesTransformationStage = { module ->
                val nestedFunctionDRI = module.packages.flatMap { it.classlikes }.flatMap { it.functions }.filter { it.name == "b" }.map { it.dri }.single()

                assertTrue(nestedFunctionDRI.classNames == "A")
            }
        }
    }

    @Test
    fun `should correctly handle object methods`() {
        testInline(
                """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |object A {
            |   fun b() {}
            |}
        """.trimMargin(),
                configuration,
        ) {
            documentablesTransformationStage = { module ->
                val nestedFunctionDRI = module.packages.flatMap { it.classlikes }.flatMap { it.functions }.filter { it.name == "b" }.map { it.dri }.single()

                assertTrue(nestedFunctionDRI.classNames == "A")
            }
        }
    }

    @Test
    fun `should correctly handle enum functions`() {
        testInline(
                """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |enum class A(private val x: Int) {
            |   X(0);
            |   fun b() = x
            |}
        """.trimMargin(),
                configuration,
        ) {
            documentablesTransformationStage = { module ->
                val nestedFunctionDRI = (module.packages.single().classlikes.single() as DEnum).functions.filter { it.name == "b" }.map { it.dri }.single()

                assertTrue(nestedFunctionDRI.classNames == "A")
            }
        }
    }

    @Test
    fun `should correctly handle nested classes' constructors`() {
        testInline(
                """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class A {
            |   class B(val x: String)
            |}
        """.trimMargin(),
                configuration,
        ) {
            documentablesTransformationStage = { module ->
                val constructorDRI = (module.packages.flatMap { it.classlikes }.flatMap { it.classlikes }.single() as DClass).constructors.single().dri
                assertTrue(constructorDRI.classNames == "A.B")
            }
        }
    }
}
