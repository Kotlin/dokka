package pageMerger

import matchers.content.*
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import utils.pWrapped
import utils.signature

class FunctionMergingContentTest : AbstractCoreTest() {
    private val testConfiguration = dokkaConfiguration {
        passes {
            pass {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
                targets = listOf("jvm")
            }
        }
    }

    @Test
    fun oneFunction() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
            |  * @param param comment to param
            |  */
            |fun onlyFunction(param: String) {
            |    println(param)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "onlyFunction" } as ContentPage
                page.content.assertNode {
                    header(1) { +"onlyFunction" }
                    signature("onlyFunction", "param" to "String")
                    header(3) { +"Description" }
                    platformHinted { pWrapped("comment to function") }
                    header(3) { +"Param" }
                    platformHinted { pWrapped("comment to param") }
                }
            }
        }
    }
}