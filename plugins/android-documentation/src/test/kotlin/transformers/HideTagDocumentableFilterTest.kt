package transformers

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DClass
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class HideTagDocumentableFilterTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src")
            }
        }
    }


    @Test
    fun `should work as hide in java with functions`() {
        testInline(
            """
            |/src/suppressed/Testing.java
            |package testing;
            |
            |public class Testing {
            |   /**
            |    * @hide
            |    */
            |   public void shouldNotBeVisible() { }
            |}
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val testingClass = modules.flatMap { it.packages }.flatMap { it.classlikes }.single() as DClass
                assertEquals(0, testingClass.functions.size)
            }
        }
    }

    @Test
    fun `should work as hide in java with classes`() {
        testInline(
            """
            |/src/suppressed/Suppressed.java
            |package testing;
            |
            |/**
            | * @hide
            | */
            |public class Suppressed {
            |}
            |/src/suppressed/Visible.java
            |package testing;
            |
            |/**
            | * Another docs
            | * @undeprecate
            | */
            |public class Visible {
            |}
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val classes = modules.flatMap { it.packages }.flatMap { it.classlikes }.map { it.name }
                assertEquals(listOf("Visible"), classes)
            }
        }
    }


}