package kotlinAsJavaPlugin

import org.jetbrains.dokka.pages.ContentGroup
import org.jetbrains.dokka.pages.ContentTable
import org.junit.Test
import testRunner.AbstractCoreTest
import kotlin.test.fail

class KotlinAsJavaPluginTest : AbstractCoreTest() {

    @Test
    fun topLevelTest() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    sourceRoots = listOf("src/")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |fun testF() {}
            |val testV = 1
        """,
            configuration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val content = root.children.firstOrNull()?.children?.firstOrNull()?.content ?: run {
                    fail("Either children or content is null")
                }

                val children =
                    if (content is ContentGroup)
                        content.children.filterIsInstance<ContentTable>().filter { it.children.isNotEmpty() }
                    else emptyList()

                children.assertCount(2)
            }
        }
    }

    @Test
    fun topLevelWithClassTest() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    sourceRoots = listOf("src/")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |class Test { 
            |   fun testFC() {}
            |   val testVC = 1
            |}
            |
            |fun testF() {}
            |val testV = 1
        """,
            configuration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val contentList = root.children.flatMap { it.children }.map { it.content }

                val children = contentList.flatMap { content ->
                    if (content is ContentGroup)
                        content.children.filterIsInstance<ContentTable>().filter { it.children.isNotEmpty() }
                    else emptyList()
                }.filterNot { it.toString().contains("<init>") }

                children.assertCount(4)
            }
        }
    }

    private fun <T> Collection<T>.assertCount(n: Int) =
        assert(count() == n) { "Expected $n, got ${count()}" }

}