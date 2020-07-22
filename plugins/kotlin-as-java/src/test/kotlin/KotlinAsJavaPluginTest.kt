package kotlinAsJavaPlugin

import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.junit.jupiter.api.Test
import matchers.content.*

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
                }

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

    @Test
    fun `public kotlin properties should have a getter with same visibilities`(){
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
            |   public val publicProperty: String = ""
            |}
        """,
            configuration,
            cleanupOutput = true
        ) {
            pagesTransformationStage = { rootPageNode ->
                val propertyGetter = rootPageNode.dfs { it is MemberPageNode && it.name == "getPublicProperty" } as? MemberPageNode
                assert(propertyGetter != null)
                propertyGetter!!.content.assertNode {
                    group {
                        header(1) {
                            +"getPublicProperty"
                        }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                group {
                                    +"final"
                                    group {
                                        link {
                                            +" String"
                                        }
                                    }
                                    link {
                                        +"getPublicProperty"
                                    }
                                    +"()"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `java properties should keep its modifiers`(){
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/TestJ.java
            |package kotlinAsJavaPlugin
            |
            |class TestJ {
            |   public Int publicProperty = 1;
            |}
        """,
            configuration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val testClass = root.dfs { it.name == "TestJ" } as? ClasslikePageNode
                assert(testClass != null)
                (testClass!!.content as ContentGroup).children.last().assertNode {
                    group {
                        header(2){
                            +"Properties"
                        }
                        table {
                            group {
                                link {
                                    +"publicProperty"
                                }
                                platformHinted {
                                    group {
                                        +"public Int"
                                        link {
                                            +"publicProperty"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `koltin interfaces and classes should be split to extends and implements`(){
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
            | open class A { }
            | interface B
            | class C : A(), B
        """,
            configuration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val testClass = root.dfs { it.name == "C" } as? ClasslikePageNode
                assert(testClass != null)
                testClass!!.content.assertNode {
                    group {
                        header(expectedLevel = 1) {
                            +"C"
                        }
                        platformHinted {
                            group {
                                +"public final class"
                                link {
                                    +"C"
                                }
                                +" extends "
                                link {
                                    +"A"
                                }
                                +" implements "
                                link {
                                    +"B"
                                }
                            }
                        }
                    }
                    skipAllNotMatching()
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
