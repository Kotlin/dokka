package kotlinAsJavaPlugin

import org.jetbrains.dokka.pages.PageNode
import org.junit.Test
import testApi.testRunner.AbstractCoreTest

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
            |fun testFL(l: List<String>) = l
            |fun testF() {}
            |fun testF2(i: Int) = i
            |fun testF3(to: TestObj) = to
            |fun <T : Char> testF4(t: T) = listOf(t)
            |val testV = 1
        """,
            configuration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                root.assertChildrenCount(1)

                val pkg = root.children.first()
                pkg.assertChildrenCount(1)

                val clazz = pkg.children.first()
                "testFL, testF, testF2, testF3, testF4, getTestV".split(",").map { it.trim() }
                    .assertSameElementsIgnoreOrder(clazz.children.map { it.name })
            }
        }
    }

    private fun PageNode.assertChildrenCount(n: Int) = children.assertCount(n, "$name:")

    private fun <T> Collection<T>.assertCount(n: Int, msgPrefix: String = "") =
        assert(count() == n) { "$msgPrefix Expected $n, got ${count()}".trim() }

    private fun <T> Collection<T>.assertSameElementsIgnoreOrder(
        col: Collection<T>,
        block: (T) -> String = { it.toString() }
    ) {
        assert(this.size == col.size) { "Collections must be equal in size, e: ${count()}, o: ${col.count()}" }
        assert(this.all { col.contains(it) }) {
            "Collections are not equal\n" +
                    "Expected: ${this.map(block)},\n" +
                    "Obtained: ${col.map(block)}"
        }
    }

}