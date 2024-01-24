/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.allModulesPage.templates

import matchers.content.*
import org.jetbrains.dokka.allModulesPage.MultiModuleAbstractTest
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentResolvedLink
import org.jetbrains.dokka.pages.MultimoduleRootPageNode
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiModuleDocumentationTest : MultiModuleAbstractTest() {

    @field:TempDir
    lateinit var tempDir: File

    val documentationContent = """
        # Sample project
        Sample documentation with [external link](https://www.google.pl)
    """.trimIndent()

    @BeforeTest
    fun setup() {
        tempDir.resolve("README.md").writeText(documentationContent)
    }

    @AfterTest
    fun teardown(){
        tempDir.resolve("README.md").delete()
    }

    @Test
    fun `documentation should be included in all modules page`() {
        val configuration = dokkaConfiguration {
            includes = listOf(tempDir.resolve("README.md"))
        }

        testFromData(configuration) {
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
