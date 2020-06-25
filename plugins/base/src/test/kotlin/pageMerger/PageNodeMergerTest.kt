package pageMerger

import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PageNode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest

class PageNodeMergerTest : AbstractCoreTest() {

    /* object SameNameStrategy : DokkaPlugin() {
        val strategy by extending { CoreExtensions.pageMergerStrategy with SameMethodNamePageMergerStrategy }
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
     */

    @Test
    fun sameNameStrategyTest() {

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
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
            configuration/*,
            pluginOverrides = listOf(SameNameStrategy)*/
        ) {
            pagesTransformationStage = {
                val allChildren = it.childrenRec().filterIsInstance<ContentPage>()
                val testT = allChildren.filter { it.name == "testT" }
                val test = allChildren.filter { it.name == "test" }

                assertTrue(testT.size == 1) { "There can be only one testT page" }
                assertTrue(testT.first().dri.size == 2) { "testT page should have 2 DRI, but has ${testT.first().dri.size}" }

                assertTrue(test.size == 1) { "There can be only one test page" }
                assertTrue(test.first().dri.size == 2) { "test page should have 2 DRI, but has ${test.first().dri.size}" }
            }
        }
    }

    @Disabled("TODO: reenable when we have infrastructure for turning off extensions")
    @Test
    fun defaultStrategyTest() {
        val strList: MutableList<String> = mutableListOf()

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
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
            configuration/*,
            pluginOverrides = listOf(DefaultStrategy(strList)) */
        ) {
            pagesTransformationStage = { root ->
                val allChildren = root.childrenRec().filterIsInstance<ContentPage>()
                val testT = allChildren.filter { it.name == "testT" }
                val test = allChildren.filter { it.name == "test" }

                assertTrue(testT.size == 1) { "There can be only one testT page" }
                assertTrue(testT.first().dri.size == 1) { "testT page should have single DRI, but has ${testT.first().dri.size}" }

                assertTrue(test.size == 1) { "There can be only one test page" }
                assertTrue(test.first().dri.size == 1) { "test page should have single DRI, but has ${test.first().dri.size}" }

                assertTrue(strList.count() == 2) { "Expected 2 warnings, got ${strList.count()}" }
            }
        }
    }

    fun PageNode.childrenRec(): List<PageNode> = listOf(this) + children.flatMap { it.childrenRec() }


    @Test
    fun `should not be merged`() {

        val configuration = dokkaConfiguration {
            sourceSets {
                val common = sourceSet {
                    moduleName = "example"
                    name = "common"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonMain/kotlin/pageMerger/Test.kt")
                }
                val js = sourceSet {
                    moduleName = "example"
                    name = "js"
                    displayName = "js"
                    analysisPlatform = "js"
                    dependentSourceSets = setOf(common.sourceSetID)
                    sourceRoots = listOf("src/jsMain/kotlin/pageMerger/Test.kt")
                }
                val jvm = sourceSet {
                    moduleName = "example"
                    name = "jvm"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    dependentSourceSets = setOf(common.sourceSetID)
                    sourceRoots = listOf("src/jvmMain/kotlin/pageMerger/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/commonMain/kotlin/pageMerger/Test.kt
            |package pageMerger
            |
            |/src/jsMain/kotlin/pageMerger/Test.kt
            |package pageMerger
            |
            |annotation class DoNotMerge
            |
            |/src/jvmMain/kotlin/pageMerger/Test.kt
            |package pageMerger
            |
            |annotation class DoNotMerge
        """.trimMargin(),
            configuration
        ) {
            pagesTransformationStage = {
                println(it)
                val allChildren = it.childrenRec().filterIsInstance<ClasslikePageNode>()
                val jvmClass = allChildren.filter { it.name == "DoNotMerge(jvm)" }
                val jsClass = allChildren.filter { it.name == "DoNotMerge(js)" }
                val noClass = allChildren.filter { it.name == "DoNotMerge" }
                assertTrue(jvmClass.size == 1) { "There can be only one DoNotMerge(jvm) page" }
                assertTrue(jvmClass.first().documentable?.sourceSets?.single()?.analysisPlatform?.key == "jvm") { "DoNotMerge(jvm) should have only jvm sources" }

                assertTrue(jsClass.size == 1) { "There can be only one DoNotMerge(js) page" }
                assertTrue(jsClass.first().documentable?.sourceSets?.single()?.analysisPlatform?.key == "js") { "DoNotMerge(js) should have only js sources" }

                assertTrue(noClass.isEmpty()) { "There can't be any DoNotMerge page" }
            }
        }
    }
}
