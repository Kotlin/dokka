package content

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.doc.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentInDescriptionTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
                classpath += jvmStdlibPath!!
            }
        }
    }

    val expectedDescription = Description(
        CustomDocTag(
            listOf(
                P(
                    listOf(
                        Text("Hello World! Docs with period issue, e.g."),
                        Text(String(Character.toChars(160)), params = mapOf("content-type" to "html")),
                        Text("this.")
                    )
                )
            ),
            params = emptyMap(),
            name = "MARKDOWN_FILE"
        )
    )

    @Test
    fun `nbsp is handled as code in kotlin`() {
        testInline(
            """
            |/src/main/kotlin/sample/ParentKt.kt
            |package sample;
            |/**
            | * Hello World! Docs with period issue, e.g.&nbsp;this.
            | */
            |public class ParentKt {
            |}
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = {
                val classlike = it.packages.flatMap { it.classlikes }.find { it.name == "ParentKt" }

                assertTrue(classlike != null)
                assertEquals(expectedDescription, classlike.documentation.values.first().children.first())
            }
        }
    }

    @Test
    fun `nbsp is handled as code in java`() {
        testInline(
            """
            |/src/main/kotlin/sample/Parent.java
            |package sample;
            |/**
            | * Hello World! Docs with period issue, e.g.&nbsp;this.
            | */
            |public class Parent {
            |}
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = {
                val classlike = it.packages.flatMap { it.classlikes }.find { it.name == "Parent" }

                assertTrue(classlike != null)
                assertEquals(expectedDescription, classlike.documentation.values.first().children.first())
            }
        }
    }

    @Test
    fun `same documentation in java and kotlin when nbsp is present`() {
        testInline(
            """
            |/src/main/kotlin/sample/Parent.java
            |package sample;
            |/**
            | * Hello World! Docs with period issue, e.g.&nbsp;this.
            | */
            |public class Parent {
            |}
            |
            |/src/main/kotlin/sample/ParentKt.kt
            |package sample;
            |/**
            | * Hello World! Docs with period issue, e.g.&nbsp;this.
            | */
            |public class ParentKt {
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val java = module.packages.flatMap { it.classlikes }.first { it.name == "Parent" }
                val kotlin = module.packages.flatMap { it.classlikes }.first { it.name == "ParentKt" }

                assertEquals(java.documentation.values.first(), kotlin.documentation.values.first())
            }
        }
    }

    @Test
    fun `text surrounded by angle brackets is not removed`() {
        testInline(
            """
        |/src/main/kotlin/sample/Foo.kt
        |package sample
        |/**
        | * My example `CodeInline<Bar>`
        | * ``` 
        | *  CodeBlock<Bar>
        | * ```         
        | */
        |class Foo {
        |}
        """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val cls = module.packages.flatMap { it.classlikes }.first { it.name == "Foo" }
                val documentation = cls.documentation.values.first()
                val docTags = documentation.children.single().root.children

                assertEquals("CodeInline<Bar>", ((docTags[0].children[1] as CodeInline).children.first() as Text).body)
                assertEquals("CodeBlock<Bar>", ((docTags[1] as CodeBlock).children.first() as Text).body)
            }
        }
    }
}