package content.receiver

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.Receiver
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.pages.ContentHeader
import org.jetbrains.dokka.pages.ContentText
import org.jetbrains.dokka.pages.MemberPageNode
import org.junit.jupiter.api.Test
import utils.docs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ContentForReceiverTest: BaseAbstractTest() {
    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    fun `should have docs for receiver`(){
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |/**
            | * docs
            | * @receiver docs for string
            | */
            |fun String.asd2(): String = this
            """.trimIndent(),
            testConfiguration
        ){
            documentablesTransformationStage = { module ->
                with(module.packages.flatMap { it.functions }.first()){
                    val receiver = docs().firstOrNull { it is Receiver }
                    assertNotNull(receiver)
                    val content = receiver.dfs { it is Text } as Text
                    assertEquals("docs for string", content.body)
                }
            }
            pagesTransformationStage = { rootPageNode ->
                val functionPage = rootPageNode.dfs { it is MemberPageNode } as MemberPageNode
                val header = functionPage.content.dfs { it is ContentHeader && it.children.firstOrNull() is ContentText }
                val text = functionPage.content.dfs { it is ContentText && it.text == "docs for string" }

                assertNotNull(header)
                assertNotNull(text)
            }
        }
    }
}