package pageMerger

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.pages.DefaultPageMergerStrategy
import org.jetbrains.dokka.transformers.pages.PageMergerStrategy
import org.jetbrains.dokka.transformers.pages.SameNamePageMergerStrategy
import org.jetbrains.dokka.utilities.DokkaLogger
import org.junit.Test
import testApi.testRunner.AbstractCoreTest

class PageNodeMergerTest : AbstractCoreTest() {

    object SameNameStrategy : DokkaPlugin() {
        val strategy by extending { CoreExtensions.pageMergerStrategy with SameNamePageMergerStrategy }
    }

    class DefaultStrategy(val strList: MutableList<String> = mutableListOf()) : DokkaPlugin(), DokkaLogger {
        val strategy by extending { CoreExtensions.pageMergerStrategy with DefaultPageMergerStrategy(this@DefaultStrategy) }

        override var warningsCount: Int = 0
        override var errorsCount: Int = 0

        override fun debug(message: String) = TODO()

        override fun info(message: String) = TODO()

        override fun progress(message: String) = TODO()

        override fun warn(message: String) {
            strList += message
        }

        override fun error(message: String) = TODO()

        override fun report() = TODO()
    }

    @Test
    fun sameNameStrategyTest() {

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
            configuration,
            pluginOverrides = listOf(SameNameStrategy)
        ) {
            pagesTransformationStage = {
                val allChildren = it.childrenRec().filterIsInstance<ContentPage>()
                val testT = allChildren.filter { it.name == "testT" }
                val test = allChildren.filter { it.name == "test" }

                assert(testT.size == 1) { "There can be only one testT page" }
                assert(testT.first().dri.size == 2) { "testT page should have 2 DRI, but has ${testT.first().dri.size}" }

                assert(test.size == 1) { "There can be only one test page" }
                assert(test.first().dri.size == 2) { "test page should have 2 DRI, but has ${test.first().dri.size}" }
            }
        }
    }

    @Test
    fun defaultStrategyTest() {
        val strList: MutableList<String> = mutableListOf()

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
            configuration,
            pluginOverrides = listOf(DefaultStrategy(strList))
        ) {
            pagesTransformationStage = { root ->
                val allChildren = root.childrenRec().filterIsInstance<ContentPage>()
                val testT = allChildren.filter { it.name == "testT" }
                val test = allChildren.filter { it.name == "test" }

                assert(testT.size == 1) { "There can be only one testT page" }
                assert(testT.first().dri.size == 1) { "testT page should have single DRI, but has ${testT.first().dri.size}" }

                assert(test.size == 1) { "There can be only one test page" }
                assert(test.first().dri.size == 1) { "test page should have single DRI, but has ${test.first().dri.size}" }

                assert(strList.count() == 2) { "Expected 2 warnings, got ${strList.count()}" }
            }
        }
    }

    fun PageNode.childrenRec(): List<PageNode> = listOf(this) + children.flatMap { it.childrenRec() }

}