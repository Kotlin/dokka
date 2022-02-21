package content.functions

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.*
import org.junit.jupiter.api.Test
import utils.assertNotNull
import kotlin.test.assertEquals

class ContentForConstructors : BaseAbstractTest() {
    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    fun `constructor name should have RowTitle style`() {
        testInline("""
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |/**
            | * Dummy text.
            | */
            |class Example(val exampleParameter: Int) {
            |}
        """.trimIndent(), testConfiguration) {
            pagesTransformationStage = { module ->
                val classPage =
                    module.dfs { it.name == "Example" && (it as WithDocumentables).documentables.firstOrNull() is DClass } as ContentPage
                val constructorsTable =
                    classPage.content.dfs { it is ContentTable && it.dci.kind == ContentKind.Constructors } as ContentTable

                assertEquals(1, constructorsTable.children.size)
                val primary = constructorsTable.children.first()
                val constructorName =
                    primary.dfs { (it as? ContentText)?.text == "Example" }.assertNotNull("constructorName")

                assert(ContentStyle.RowTitle in constructorName.style)
            }
        }
    }
}