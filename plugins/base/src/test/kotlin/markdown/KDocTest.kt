package markdown

import org.jetbrains.dokka.model.Package
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.pages.ModulePageNode
import org.junit.Assert
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest

open class KDocTest : AbstractCoreTest() {

    private val configuration = dokkaConfiguration {
        passes {
            pass {
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
        (modulePageNode.documentable?.children?.first() as Package)
            .classlikes.single()
            .documentation.values.single()


    protected fun executeTest(kdoc: String, expectedDocumentationNode: DocumentationNode) {
        testInline(
            interpolateKdoc(kdoc),
            configuration
        ) {
            pagesGenerationStage = {
                Assert.assertEquals(
                    expectedDocumentationNode,
                    actualDocumentationNode(it)
                )
            }
        }
    }
}