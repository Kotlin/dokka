/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package markdown

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.pages.ModulePageNode
import kotlin.test.assertEquals

abstract class KDocTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin/example/Test.kt")
            }
        }
    }

    private fun interpolateKdoc(kdoc: String) = """
            |/src/main/kotlin/example/Test.kt
            |package example
            | /**
            ${kdoc.split("\n").joinToString("") { "| *$it\n" } }
            | */
            |class Test
        """.trimMargin()

    private fun actualDocumentationNode(modulePageNode: ModulePageNode) =
        (modulePageNode.documentables.firstOrNull()?.children?.first() as DPackage)
            .classlikes.single()
            .documentation.values.single()


    protected fun executeTest(kdoc: String, expectedDocumentationNode: DocumentationNode) {
        testInline(
            interpolateKdoc(kdoc),
            configuration
        ) {
            pagesGenerationStage = {
                assertEquals(
                    expectedDocumentationNode,
                    actualDocumentationNode(it as ModulePageNode)
                )
            }
        }
    }
}
