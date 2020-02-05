package pageMerger

import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PageNode
import org.junit.Test
import testApi.testRunner.AbstractCoreTest

class DefaultPageNodeMergerTest : AbstractCoreTest() {

    @Test
    fun test1() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    sourceRoots = listOf("src/main/kotlin/pageMerger/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/pageMerger/Test.kt
            |package pageMerger
            |
            |fun testT(): Int = 1
            |fun testT(i: Int): Int = i
            |
            |object Test {
            |   fun test(): String = ""
            |   fun test(str: String): String = str
            |}
        """.trimMargin(),
            configuration
        ) {
            pagesTransformationStage = {
                val allChildren = it.childrenRec().filterIsInstance<ContentPage>()
                val testT = allChildren.filter { it.name == "testT" }
                val test = allChildren.filter { it.name == "test" }

                assert(testT.size == 1) { "There can be only one testT page" }
                assert(testT.first().dri.size == 2) { "testT page should have 2 DRI" }

                assert(test.size == 1) { "There can be only one test page" }
                assert(test.first().dri.size == 2) { "test page should have 2 DRI" }
            }
        }
    }

    fun PageNode.childrenRec(): List<PageNode> = listOf(this) + children.flatMap { it.childrenRec() }

}