/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package parsers.javadoc

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.doc.CodeInline
import org.jetbrains.dokka.model.doc.DocumentationLink
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

    @Test
    fun `markdown reference link`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example
            |
            | /// a package [java.util]
            | /// a class [String]
            | /// a field [String#CASE_INSENSITIVE_ORDER]
            | /// a method [String#chars()]
            | /// [the java.util package][java.util]
            | /// [a class][String]
            | /// [a field][String#CASE_INSENSITIVE_ORDER]
            | /// [a method][String#chars()]
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
                                Text("a package "),
                                DocumentationLink(
                                    dri = DRI(packageName = "java.util"),
                                    children = listOf(Text("java.util"))
                                ),
                                Text(" a class "),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.lang",
                                        classNames = "String"
                                    ),
                                    children = listOf(Text("String"))
                                ),
                                Text(" a field "),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.lang",
                                        classNames = "String",
                                        callable = Callable(
                                            name = "CASE_INSENSITIVE_ORDER",
                                            params = emptyList(),
                                            isProperty = true
                                        )
                                    ),
                                    children = listOf(Text("String#CASE_INSENSITIVE_ORDER"))
                                ),
                                Text(" a method "),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.lang",
                                        classNames = "CharSequence",
                                        callable = Callable(
                                            name = "chars",
                                            params = emptyList()
                                        )
                                    ),
                                    children = listOf(Text("String#chars()"))
                                ),
                                DocumentationLink(
                                    dri = DRI(packageName = "java.util"),
                                    children = listOf(Text("the java.util package"))
                                ),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.lang",
                                        classNames = "String"
                                    ),
                                    children = listOf(Text("a class"))
                                ),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.lang",
                                        classNames = "String",
                                        callable = Callable(
                                            name = "CASE_INSENSITIVE_ORDER",
                                            params = emptyList(),
                                            isProperty = true
                                        )
                                    ),
                                    children = listOf(Text("a field"))
                                ),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.lang",
                                        classNames = "CharSequence",
                                        callable = Callable(
                                            name = "chars",
                                            params = emptyList()
                                        )
                                    ),
                                    children = listOf(Text("a method"))
                                )
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
