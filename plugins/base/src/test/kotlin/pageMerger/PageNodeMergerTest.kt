package pageMerger

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.*
import org.junit.jupiter.api.RepeatedTest
import java.lang.IllegalArgumentException
import kotlin.test.assertEquals

class PageNodeMergerTest : BaseAbstractTest() {

    private val defaultConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    @Test
    fun sameNameStrategyTest() {
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
            defaultConfiguration
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
            defaultConfiguration
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
            moduleName = "example"
            sourceSets {
                val common = sourceSet {
                    name = "common"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonMain/kotlin/pageMerger/Test.kt")
                }
                sourceSet {
                    name = "js"
                    displayName = "js"
                    analysisPlatform = "js"
                    dependentSourceSets = setOf(common.value.sourceSetID)
                    sourceRoots = listOf("src/jsMain/kotlin/pageMerger/Test.kt")
                }
                sourceSet {
                    name = "jvm"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    dependentSourceSets = setOf(common.value.sourceSetID)
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
                val jvmClass = allChildren.filter { it.name == "[jvm]DoNotMerge" }
                val jsClass = allChildren.filter { it.name == "[js]DoNotMerge" }
                val noClass = allChildren.filter { it.name == "DoNotMerge" }
                assertTrue(jvmClass.size == 1) { "There can be only one DoNotMerge(jvm) page" }
                assertTrue(jvmClass.first().documentables.firstOrNull()?.sourceSets?.single()?.analysisPlatform?.key == "jvm") { "[jvm]DoNotMerge should have only jvm sources" }

                assertTrue(jsClass.size == 1) { "There can be only one DoNotMerge(js) page" }
                assertTrue(jsClass.first().documentables.firstOrNull()?.sourceSets?.single()?.analysisPlatform?.key == "js") { "[js]DoNotMerge should have only js sources" }

                assertTrue(noClass.isEmpty()) { "There can't be any DoNotMerge page" }
            }
        }
    }

    @RepeatedTest(3)
    fun `should deterministically render same name property extensions`() {
        testInline(
            """
            |/src/main/kotlin/test/Test.kt
            |package test
            |
            |class ExtensionReceiver
            |
            |/**
            | * Top level val extension
            | */
            |val ExtensionReceiver.foo: String get() = "bar"
            |
            |class Obj {
            |    companion object {
            |        /**
            |         * Companion val extension
            |         */
            |        val ExtensionReceiver.foo: String get() = "bar"
            |    }
            |}
            |
            |/src/main/kotlin/test/nestedpackage/Pckg.kt
            |package test.nestedpackage
            |
            |import test.ExtensionReceiver
            |
            |/**
            | * From nested package int val extension
            | */
            |val ExtensionReceiver.foo: Int get() = 42
        """.trimMargin(),
            defaultConfiguration
        ) {
            renderingStage = { rootPageNode, _ ->
                val extensions = rootPageNode.findExtensionsOfClass("ExtensionReceiver")

                extensions.assertContainsKDocsInOrder(
                    "Top level val extension",
                    "Companion val extension",
                    "From nested package int val extension"
                )
            }
        }
    }

    @RepeatedTest(3)
    fun `should deterministically render parameterless same name function extensions`() {
        testInline(
            """
            |/src/main/kotlin/test/Test.kt
            |package test
            |
            |class ExtensionReceiver
            |
            |/**
            | * Top level fun extension
            | */
            |fun ExtensionReceiver.bar(): String = "bar"
            |
            |class Obj {
            |
            |    companion object {
            |        /**
            |         * Companion fun extension
            |         */
            |        fun ExtensionReceiver.bar(): String = "bar"
            |    }
            |}
            |
            |/src/main/kotlin/test/nestedpackage/Pckg.kt
            |package test.nestedpackage
            |
            |import test.ExtensionReceiver
            |
            |/**
            | * From nested package fun extension
            | */
            |fun ExtensionReceiver.bar(): String = "bar"
        """.trimMargin(),
            defaultConfiguration
        ) {
            renderingStage = { rootPageNode, _ ->
                val extensions = rootPageNode.findExtensionsOfClass("ExtensionReceiver")
                extensions.assertContainsKDocsInOrder(
                    "Top level fun extension",
                    "Companion fun extension",
                    "From nested package fun extension"
                )
            }
        }
    }

    @RepeatedTest(3)
    fun `should deterministically render same name function extensions with parameters`() {
        testInline(
            """
            |/src/main/kotlin/test/Test.kt
            |package test
            |
            |class ExtensionReceiver
            |
            |/**
            | * Top level fun extension with one string param
            | */
            |fun ExtensionReceiver.bar(one: String): String = "bar"
            |
            |/**
            | * Top level fun extension with one int param
            | */
            |fun ExtensionReceiver.bar(one: Int): Int = 42
            |
            |class Obj {
            |
            |    companion object {
            |        /**
            |         * Companion fun extension with two params
            |         */
            |        fun ExtensionReceiver.bar(one: String, two: String): String = "bar"
            |    }
            |}
            |
            |/src/main/kotlin/test/nestedpackage/Pckg.kt
            |package test.nestedpackage
            |
            |import test.ExtensionReceiver
            |
            |/**
            | * From nested package fun extension with two params
            | */
            |fun ExtensionReceiver.bar(one: String, two: String): String = "bar"
            |
            |/**
            | * From nested package fun extension with three params
            | */
            |fun ExtensionReceiver.bar(one: String, two: String, three: String): String = "bar"
            |
            |/**
            | * From nested package fun extension with four params
            | */
            |fun ExtensionReceiver.bar(one: String, two: String, three: String, four: String): String = "bar"
        """.trimMargin(),
            defaultConfiguration
        ) {
            renderingStage = { rootPageNode, _ ->
                val extensions = rootPageNode.findExtensionsOfClass("ExtensionReceiver")
                extensions.assertContainsKDocsInOrder(
                    "Top level fun extension with one int param",
                    "Top level fun extension with one string param",
                    "Companion fun extension with two params",
                    "From nested package fun extension with two params",
                    "From nested package fun extension with three params",
                    "From nested package fun extension with four params"
                )
            }
        }
    }

    @RepeatedTest(3)
    fun `should deterministically render same name function extensions with different receiver and return type`() {
        testInline(
            """
            |/src/main/kotlin/test/Test.kt
            |package test
            |
            |/**
            | * Top level fun extension string
            | */
            |fun Int.bar(): String = "bar"
            |
            |/**
            | * Top level fun extension int
            | */
            |fun String.bar(): Int = 42
        """.trimMargin(),
            defaultConfiguration
        ) {
            renderingStage = { rootPageNode, _ ->
                val packageFunctionBlocks = rootPageNode.findPackageFunctionBlocks(packageName = "test")
                assertEquals(1, packageFunctionBlocks.size, "Expected to find only one group for the functions")

                val functionsBlock = packageFunctionBlocks[0]
                functionsBlock.assertContainsKDocsInOrder(
                    "Top level fun extension string",
                    "Top level fun extension int"
                )
            }
        }
    }

    @Test
    fun `should not ignore case when grouping by name`() {
        testInline(
            """
            |/src/main/kotlin/test/Test.kt
            |package test
            |
            |/**
            | * Top level fun bAr
            | */
            |fun Int.bAr(): String = "bar"
            |
            |/**
            | * Top level fun BaR
            | */
            |fun String.BaR(): Int = 42
        """.trimMargin(),
            defaultConfiguration
        ) {
            renderingStage = { rootPageNode, _ ->
                val packageFunctionBlocks = rootPageNode.findPackageFunctionBlocks(packageName = "test")
                assertEquals(2, packageFunctionBlocks.size, "Expected two separate function groups")

                val firstGroup = packageFunctionBlocks[0]
                firstGroup.assertContainsKDocsInOrder(
                    "Top level fun BaR",
                )

                val secondGroup = packageFunctionBlocks[1]
                secondGroup.assertContainsKDocsInOrder(
                    "Top level fun bAr",
                )
            }
        }
    }

    @Test
    fun `should sort groups alphabetically ignoring case`() {
        testInline(
            """
            |/src/main/kotlin/test/Test.kt
            |package test
            |
            |/** Sequence builder */
            |fun <T> sequence(): Sequence<T>
            |
            |/** Sequence SAM constructor */
            |fun <T> Sequence(): Sequence<T>
            |
            |/** Sequence.any() */
            |fun <T> Sequence<T>.any() {}
            |
            |/** Sequence interface */
            |interface Sequence<T>
        """.trimMargin(),
            defaultConfiguration
        ) {
            renderingStage = { rootPageNode, _ ->
                val packageFunctionBlocks = rootPageNode.findPackageFunctionBlocks(packageName = "test")
                assertEquals(3, packageFunctionBlocks.size, "Expected 3 separate function groups")

                packageFunctionBlocks[0].assertContainsKDocsInOrder(
                    "Sequence.any()",
                )

                packageFunctionBlocks[1].assertContainsKDocsInOrder(
                    "Sequence SAM constructor",
                )

                packageFunctionBlocks[2].assertContainsKDocsInOrder(
                    "Sequence builder",
                )
            }
        }
    }

    private fun RootPageNode.findExtensionsOfClass(name: String): ContentDivergentGroup {
        val extensionReceiverPage = this.dfs { it is ClasslikePageNode && it.name == name } as ClasslikePageNode
        return extensionReceiverPage.content
            .dfs { it is ContentDivergentGroup && it.groupID.name == "Extensions" } as ContentDivergentGroup
    }

    private fun RootPageNode.findPackageFunctionBlocks(packageName: String): List<ContentDivergentGroup> {
        val packagePage = this.dfs { it is PackagePage && it.name == packageName } as PackagePage
        val packageFunctionTable = packagePage.content.dfs {
            it is ContentTable && it.dci.kind == ContentKind.Functions
        } as ContentTable

        return packageFunctionTable.children.map { packageGroup ->
            packageGroup.dfs { it is ContentDivergentGroup } as ContentDivergentGroup
        }
    }

    private fun ContentDivergentGroup.assertContainsKDocsInOrder(vararg expectedKDocs: String) {
        expectedKDocs.forEachIndexed { index, expectedKDoc ->
            assertEquals(expectedKDoc, this.getElementKDocText(index))
        }
    }

    private fun ContentDivergentGroup.getElementKDocText(index: Int): String {
        val element = this.children.getOrNull(index) ?: throw IllegalArgumentException("No element with index $index")
        val commentNode = element.after
            ?.withDescendants()
            ?.singleOrNull { it is ContentText && it.dci.kind == ContentKind.Comment }
            ?: throw IllegalStateException("Expected the element to contain a single paragraph of text / comment")

        return (commentNode as ContentText).text
    }
}
