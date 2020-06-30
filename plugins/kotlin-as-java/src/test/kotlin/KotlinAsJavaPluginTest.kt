package kotlinAsJavaPlugin

import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.junit.jupiter.api.Test

class KotlinAsJavaPluginTest : AbstractCoreTest() {

    @Test
    fun topLevelTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
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
                val content = (root.children.single().children.first { it.name == "TestKt" } as ContentPage).content

                val children = content.mainContents.first().cast<ContentGroup>()
                    .children.filterIsInstance<ContentTable>()
                    .filter { it.children.isNotEmpty() }

                children.assertCount(2)
            }
        }
    }

    @Test
    fun topLevelWithClassTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
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
                    content.mainContents.first().cast<ContentGroup>().children
                        .filterIsInstance<ContentTable>()
                        .filter { it.children.isNotEmpty() }
                }.filterNot { it.toString().contains("<init>") }

                children.assertCount(4)
            }
        }
    }

    @Test
    fun kotlinAndJavaTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
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

private val ContentNode.mainContents: List<ContentNode>
    get() = (this as ContentGroup).children
    .filterIsInstance<ContentGroup>()
    .single { it.dci.kind == ContentKind.Main }.children
