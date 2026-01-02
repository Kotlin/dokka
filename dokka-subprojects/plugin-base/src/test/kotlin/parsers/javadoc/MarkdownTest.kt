/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package parsers.javadoc

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.doc.CodeInline
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Pre
import org.jetbrains.dokka.model.doc.Text
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    // TODO update test after the merge of https://github.com/Kotlin/dokka/pull/4392
    @Test
    fun `markdown code block`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example
            |
            | /// Inline `code` block.
            | /// Traditional code block:
            | /// ```
            | /// /** Hello World! */
            | /// public class HelloWorld {
            | ///     public static void main(String... args) {
            | ///         System.out.println("Hello World!"); // the traditional example
            | ///     }
            | /// }
            | /// ```
            | /// Code block with specified language:
            | /// ```kotlin
            | /// val sum: (Int, Int) -> Int = { x: Int, y: Int -> x + y }
            | /// ```
            | public class Test {}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        P(
                            children = listOf(
                                Text("Inline "),
                                CodeInline(
                                    children = listOf(Text("code")),
                                    params = mapOf("lang" to "java")
                                ),
                                Text(" block. Traditional code block: ")
                            )
                        ),
                        Pre(
                            children = listOf(
                                Text("/** Hello World! */\npublic class HelloWorld {\n    public static void main(String... args) {\n        System.out.println(\"Hello World!\"); // the traditional example\n    }\n}")
                            )
                        ),
                        Text(" Code block with specified language: "),
                        Pre(
                            children = listOf(
                                Text("val sum: (Int, Int) -> Int = { x: Int, y: Int -> x + y }")
                            )
                        )
                    ),
                    root.children
                )
            }
        }
    }

    private fun getFirstClassDocRoot(modules: List<DModule>) =
        modules.first().packages.first().classlikes.single().documentation.values.first().children.first().root
}
