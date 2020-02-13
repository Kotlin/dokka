package snippets

import org.jetbrains.dokka.pages.MemberPageNode
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RendererSpecificResourcePage
import org.junit.Test
import testApi.testRunner.AbstractCoreTest

class SnippetsTest : AbstractCoreTest() {

    @Test
    fun snippetsTest() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    sourceRoots = listOf("src/main/kotlin/snippets/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/snippets/Test.kt
            |package multiplatform
            |
            | /**
            | * ```
            | * fun test() = 1
            | * ```
            | **/
            |fun test() = 1
        """.trimMargin(),
            configuration
        ) {
            pagesTransformationStage = {
                fun PageNode.getMembers(): List<PageNode> = when {
                    children.any { it is MemberPageNode } -> children.filter { it is MemberPageNode }
                    else -> children.flatMap { it.getMembers() }
                }

                val f = it.getMembers()
                f.assertCount(1, "Members: ")

                f.first().let { f ->
                    f.children.assertCount(1, "Method children: ")

                    f.children.find { it is RendererSpecificResourcePage }
                        ?.let { assert(it.name == "test-0") { "Expected name: test-0, got: ${it.name}" } }
                        ?: run { fail("RenderSpecificResourcePage not found") }
                }
            }
        }
    }

    private fun <T> Collection<T>.assertCount(n: Int, prefix: String = "") =
        assert(count() == n) { "${prefix}Expected $n, got ${count()}" }

    fun fail(msg: String) = assert(false) { msg }
}