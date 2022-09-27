package transformers

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.childrenOfType
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.firstChildOfType
import org.jetbrains.dokka.pages.*
import org.jetbrains.kotlin.utils.addIfNotNull
import org.junit.jupiter.api.Test
import utils.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MergeImplicitExpectActualDeclarationsTest : BaseAbstractTest() {

    @Suppress("UNUSED_VARIABLE")
    private fun configuration(switchOn: Boolean) = dokkaConfiguration {
        sourceSets {
            val common = sourceSet {
                name = "common"
                displayName = "common"
                analysisPlatform = "common"
                sourceRoots = listOf("src/commonMain/kotlin/pageMerger/Test.kt")
            }
            val js = sourceSet {
                name = "js"
                displayName = "js"
                analysisPlatform = "js"
                dependentSourceSets = setOf(common.value.sourceSetID)
                sourceRoots = listOf("src/jsMain/kotlin/pageMerger/Test.kt")
            }
            val jvm = sourceSet {
                name = "jvm"
                displayName = "jvm"
                analysisPlatform = "jvm"
                sourceRoots = listOf("src/jvmMain/kotlin/pageMerger/Test.kt")
            }
        }
        pluginsConfigurations.addIfNotNull(
            PluginConfigurationImpl(
                DokkaBase::class.qualifiedName!!,
                DokkaConfiguration.SerializationFormat.JSON,
                """{ "mergeImplicitExpectActualDeclarations": $switchOn }""",
            )
        )
    }

    private fun ClasslikePageNode.findSectionWithName(name: String) : ContentNode? {
        var sectionHeader: ContentHeader? = null
        return content.dfs { node ->
            node.children.filterIsInstance<ContentHeader>().any { header ->
                header.children.firstOrNull { it is ContentText && it.text == name }?.also { sectionHeader = header } != null
            }
        }?.children?.dropWhile { child -> child != sectionHeader  }?.drop(1)?.firstOrNull()
    }

    @Test
    fun `should merge fun`() {
        testInline(
            """

                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |class classA {
                |   fun method1(): String
                |}
                |
                |/src/jsMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |class classA {
                |   fun method1(): Int
                |}
                |
        """.trimMargin(),
            configuration(true),
            cleanupOutput = true
        ) {
            pagesTransformationStage = { root ->
                val classPage = root.dfs { it.name == "classA" } as? ClasslikePageNode
                assertNotNull(classPage, "Tested class not found!")

                val functions = classPage.findSectionWithName("Functions").assertNotNull("Functions")
                val method1 = functions.children.singleOrNull().assertNotNull("method1")

                assertEquals(
                    2,
                    method1.firstChildOfType<ContentDivergentGroup>().childrenOfType<ContentDivergentInstance>().size,
                    "Incorrect number of divergent instances found"
                )

                val methodPage = root.dfs { it.name == "method1" } as? MemberPageNode
                assertNotNull(methodPage, "Tested method not found!")

                val divergentGroup = methodPage.content.dfs { it is ContentDivergentGroup } as? ContentDivergentGroup

                assertEquals(
                    2,
                    divergentGroup?.childrenOfType<ContentDivergentInstance>()?.size,
                    "Incorrect number of divergent instances found in method page"
                )
            }
        }
    }

    @Test
    fun `should merge method and prop`() {
        testInline(
            """
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |class classA {
                |   fun method1(): String
                |}
                |
                |/src/jsMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |class classA {
                |   val prop1: Int
                |}
                |
        """.trimMargin(),
            configuration(true),
            cleanupOutput = true
        ) {
            pagesTransformationStage = { root ->
                val classPage = root.dfs { it.name == "classA" } as? ClasslikePageNode
                assertNotNull(classPage, "Tested class not found!")

                val props = classPage.findSectionWithName("Properties").assertNotNull("Properties")
                props.children.singleOrNull().assertNotNull("prop1")

                val functions = classPage.findSectionWithName("Functions").assertNotNull("Functions")
                functions.children.singleOrNull().assertNotNull("method1")
            }
        }
    }

    @Test
    fun `should merge prop`() {
        testInline(
            """
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |class classA {
                |   val prop1: String
                |}
                |
                |/src/jsMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |class classA {
                |   val prop1: Int
                |}
                |
        """.trimMargin(),
            configuration(true),
            cleanupOutput = true
        ) {
            pagesTransformationStage = { root ->
                val classPage = root.dfs { it.name == "classA" } as? ClasslikePageNode
                assertNotNull(classPage, "Tested class not found!")

                val props = classPage.findSectionWithName("Properties").assertNotNull("Properties")
                val prop1 = props.children.singleOrNull().assertNotNull("prop1")

                assertEquals(
                    2,
                    prop1.firstChildOfType<PlatformHintedContent>().inner.children.size,
                    "Incorrect number of divergent instances found"
                )

                val propPage = root.dfs { it.name == "prop1" } as? MemberPageNode
                assertNotNull(propPage, "Tested method not found!")

                val divergentGroup = propPage.content.dfs { it is ContentDivergentGroup } as? ContentDivergentGroup

                assertEquals(
                    2,
                    divergentGroup?.childrenOfType<ContentDivergentInstance>()?.size,
                    "Incorrect number of divergent instances found in method page"
                )
            }
        }
    }

    @Test
    fun `should merge enum and class`() {
        testInline(
            """
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |class classA {
                |   val prop1: String
                |}
                |
                |/src/jsMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |enum class classA {
                |   ENTRY
                |}
                |
        """.trimMargin(),
            configuration(true),
            cleanupOutput = true
        ) {
            pagesTransformationStage = { root ->
                val classPage = root.dfs { it.name == "classA" } as? ClasslikePageNode
                assertNotNull(classPage, "Tested class not found!")

                val entries = classPage.findSectionWithName("Entries").assertNotNull("Entries")
                entries.children.singleOrNull().assertNotNull("ENTRY")

                val props = classPage.findSectionWithName("Properties").assertNotNull("Properties")
                assertEquals(
                    3,
                    props.children.size,
                    "Incorrect number of properties found in method page"
                )
            }
        }
    }

    fun PageNode.childrenRec(): List<PageNode> = listOf(this) + children.flatMap { it.childrenRec() }

    @Test
    fun `should merge enum entries`() {
        testInline(
            """
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |enum class classA {
                |   SMTH;
                |   fun method1(): Int
                |}
                |
                |/src/jsMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |enum class classA {
                |   SMTH;
                |   fun method1(): Int
                |}
                |
        """.trimMargin(),
            configuration(true),
            cleanupOutput = true
        ) {
            pagesTransformationStage = { root ->
                val classPage = root.dfs { it.name == "SMTH" } as? ClasslikePageNode
                assertNotNull(classPage, "Tested class not found!")

                val functions = classPage.findSectionWithName("Functions").assertNotNull("Functions")
                val method1 = functions.children.singleOrNull().assertNotNull("method1")

                assertEquals(
                    2,
                    method1.firstChildOfType<ContentDivergentGroup>().childrenOfType<ContentDivergentInstance>().size,
                    "Incorrect number of divergent instances found"
                )
            }
        }
    }

    /**
     * There is a case when a property and fun from different source sets
     * have the same name so pages have the same urls respectively.
     */
    @Test
    fun `should no merge prop and method with the same name`() {
        testInline(
            """
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |class classA {
                |   fun merged():String
                |}
                |
                |/src/jsMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |class classA {
                |   val merged:String
                |}
                |
        """.trimMargin(),
            configuration(true),
            cleanupOutput = true
        ) {
            pagesTransformationStage = { root ->
                val allChildren = root.childrenRec().filterIsInstance<MemberPageNode>()

                assertEquals(
                    1,
                    allChildren.filter { it.name == "merged" }.size,
                    "Incorrect number of fun pages"
                )
            }
        }
    }

    @Test
    fun `should always merge constructor`() {
        testInline(
            """
                |/src/commonMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |expect class classA(a: Int)
                |
                |/src/jsMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual class classA(a: Int)
        """.trimMargin(),
            configuration(false),
            cleanupOutput = true
        ) {
            pagesTransformationStage = { root ->
                val classPage = root.dfs { it.name == "classA" } as? ClasslikePageNode
                assertNotNull(classPage, "Tested class not found!")

                val constructors = classPage.findSectionWithName("Constructors").assertNotNull("Constructors")

                assertEquals(
                    1,
                    constructors.children.size,
                    "Incorrect number of constructors"
                )

                val platformHinted = constructors.dfs { it is PlatformHintedContent } as? PlatformHintedContent

                assertEquals(
                    2,
                    platformHinted?.sourceSets?.size,
                    "Incorrect number of source sets"
                )
            }
        }
    }
}