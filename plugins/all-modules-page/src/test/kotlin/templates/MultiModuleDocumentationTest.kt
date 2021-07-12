package org.jetbrains.dokka.allModulesPage.templates

import matchers.content.*
import org.jetbrains.dokka.allModulesPage.MultiModuleAbstractTest
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentResolvedLink
import org.jetbrains.dokka.pages.MultimoduleRootPageNode
import org.junit.Rule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

class MultiModuleDocumentationTest : MultiModuleAbstractTest() {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    val documentationContent = """
        # Sample project
        Sample documentation with [external link](https://www.google.pl)
    """.trimIndent()

    @BeforeEach
    fun setup() {
        folder.create()
        folder.root.resolve("README.md").writeText(documentationContent)
    }

    @AfterEach
    fun teardown(){
        folder.root.resolve("README.md").delete()
    }

    @Test
    fun `documentation should be included in all modules page`() {
        val configuration = dokkaConfiguration {
            includes = listOf(folder.root.resolve("README.md"))
        }

        testFromData(configuration, preserveOutputLocation = true) {
            allModulesPageCreationStage = { rootPage ->
                (rootPage as? MultimoduleRootPageNode)?.content?.dfs { it.dci.kind == ContentKind.Cover }?.children?.firstOrNull()
                    ?.assertNode {
                        group {
                            group {
                                group {
                                    header(1) {
                                        +"Sample project"
                                    }
                                    group {
                                        +"Sample documentation with "
                                        link {
                                            +"external link"
                                            check {
                                                assertEquals(
                                                    "https://www.google.pl",
                                                    (this as ContentResolvedLink).address
                                                )
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
}
