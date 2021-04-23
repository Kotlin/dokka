package filter

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AnnotationFilterTest : BaseAbstractTest() {
    @Test
    fun `function with empty global suppressAnnotations`() {
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
            |package example
            |
            |fun testFunction() { }
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                Assertions.assertTrue(
                    it.first().packages.first().functions.size == 1
                )
            }
        }
    }

    @Test
    fun `annotated function with empty global suppressAnnotations`() {
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
            |package example
            |
            |@Deprecated("dep")
            |fun testFunction() { }
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                Assertions.assertTrue(
                    it.first().packages.first().functions.size == 1
                )
            }
        }
    }

    @Test
    fun `qualified name annotated function with used global suppressAnnotations`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    suppressedAnnotations = listOf("kotlin.ExperimentalStdlibApi")
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |@kotlin.ExperimentalStdlibApi
            |fun testFunction() { }
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                Assertions.assertTrue(
                    it.first().packages.first().functions.size == 0
                )
            }
        }
    }

    @Test
    fun `import annotated function with used global suppressAnnotations`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    suppressedAnnotations = listOf("kotlin.ExperimentalStdlibApi")
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |import kotlin.ExperimentalStdlibApi
            |
            |@ExperimentalStdlibApi
            |fun testFunction() { }
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                Assertions.assertTrue(
                    it.first().packages.first().functions.size == 0
                )
            }
        }
    }
}
