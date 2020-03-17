package kotlinAsJavaPlugin

import org.jetbrains.dokka.pages.ContentGroup
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.ContentTable
import org.jetbrains.dokka.pages.children
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test

class KotlinAsJavaPluginTest : AbstractCoreTest() {

    fun fail(msg: String) = assert(false) { msg }

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
            |object TestObj {}
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
                val content = (root.children.firstOrNull()?.children?.firstOrNull() as? ContentPage)?.content ?: run {
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
            |fun testF(i: Int) = i
            |val testV = 1
        """,
            configuration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val contentList = root.children
                    .flatMap { it.children<ContentPage>() }
                    .map { it.content }

                val children = contentList.flatMap { content ->
                    if (content is ContentGroup)
                        content.children.filterIsInstance<ContentTable>().filter { it.children.isNotEmpty() }
                    else emptyList()
                }.filterNot { it.toString().contains("<init>") }

                children.assertCount(4)
            }
        }
    }

    @Test
    fun kotlinAndJavaTest() {
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
            |fun testF(i: Int) = i
            |/src/main/kotlin/kotlinAsJavaPlugin/TestJ.java
            |package kotlinAsJavaPlugin
            |
            |class TestJ {
            |   int testF(int i) { return i; }
            |}
        """,
            configuration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val classes = root.children.first().children.associateBy { it.name }
                classes.values.assertCount(2, "Class count: ")

                classes["TestKt"].let {
                    it?.children.orEmpty().assertCount(1, "(Kotlin) TestKt members: ")
                    it!!.children.first()
                        .let { assert(it.name == "testF") { "(Kotlin) Expected method name: testF, got: ${it.name}" } }
                }

                classes["TestJ"].let {
                    it?.children.orEmpty().assertCount(1, "(Java) TestJ members: ")
                    it!!.children.first()
                        .let { assert(it.name == "testF") { "(Java) Expected method name: testF, got: ${it.name}" } }
                }
            }
        }
    }

    private fun <T> Collection<T>.assertCount(n: Int, prefix: String = "") =
        assert(count() == n) { "${prefix}Expected $n, got ${count()}" }

}