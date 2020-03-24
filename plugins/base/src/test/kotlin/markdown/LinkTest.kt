package markdown

import org.jetbrains.dokka.pages.ContentDRILink
import org.jetbrains.dokka.pages.MemberPageNode
import org.jetbrains.dokka.pages.dfs
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LinkTest : AbstractCoreTest() {
    @Test
    fun linkToClassLoader() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    sourceRoots = listOf("src/main/kotlin/parser")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/parser/Test.kt
            |package parser
            |
            | /**
            | * Some docs that link to [ClassLoader.clearAssertionStatus]
            | */
            |fun test(x: ClassLoader) = x.clearAssertionStatus()
            |
        """.trimMargin(),
            configuration
        ) {
            renderingStage = { rootPageNode, _ ->
                (rootPageNode.children.single().children.single() as MemberPageNode)
                    .content
                    .dfs { node -> node is ContentDRILink }
                    .let {
                        assertEquals(
                            "parser//test/#java.lang.ClassLoader//",
                            (it as ContentDRILink).address.toString()
                        )
                    }
            }
        }
    }
}