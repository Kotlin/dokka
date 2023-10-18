/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.*
import kotlin.test.Test
import kotlin.test.assertTrue

class HighlightingTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOf(commonStdlibPath!!, jvmStdlibPath!!)
                externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
            }
        }
    }

    @Test
    fun `open suspend fun`() {
        testInline(
            """
            |/src/main/kotlin/test/Test.kt
            |package example
            |
            | open suspend fun simpleFun(): String = "Celebrimbor"
            """,
            configuration
        ) {
            pagesTransformationStage = { module ->
                val symbol = (module.dfs { it.name == "simpleFun" } as MemberPageNode).content
                    .dfs { it is ContentGroup && it.dci.kind == ContentKind.Symbol }
                val children = symbol?.children

                for (it in listOf(
                    Pair(0, TokenStyle.Keyword), Pair(1, TokenStyle.Keyword), Pair(2, TokenStyle.Keyword),
                    Pair(4, TokenStyle.Punctuation), Pair(5, TokenStyle.Punctuation), Pair(6, TokenStyle.Operator)
                ))
                    assertTrue(children?.get(it.first)?.style?.contains(it.second) == true)
                assertTrue(children?.get(3)?.children?.first()?.style?.contains(TokenStyle.Function) == true)
            }
        }
    }

    @Test
    fun `plain typealias of plain class with annotation`() {
        testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |@MustBeDocumented
                |@Target(AnnotationTarget.TYPEALIAS)
                |annotation class SomeAnnotation
                |
                |@SomeAnnotation
                |typealias PlainTypealias = Int
                |
            """.trimMargin(),
            configuration
        ) {
            pagesTransformationStage = { module ->
                val symbol = (module.dfs { it.name == "example" } as PackagePageNode).content
                    .dfs { it is ContentGroup && it.dci.kind == ContentKind.Symbol }
                val children = symbol?.children

                for (it in listOf(
                    Pair(1, TokenStyle.Keyword), Pair(3, TokenStyle.Operator)
                ))
                    assertTrue(children?.get(it.first)?.style?.contains(it.second) == true)
                val annotation = children?.first()?.children?.first()

                assertTrue(annotation?.children?.get(0)?.style?.contains(TokenStyle.Annotation) == true)
                assertTrue(annotation?.children?.get(1)?.children?.first()?.style?.contains(TokenStyle.Annotation) == true)
            }
        }
    }
}
