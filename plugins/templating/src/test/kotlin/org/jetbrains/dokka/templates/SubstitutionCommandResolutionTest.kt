/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.templates

import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.span
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import org.jetbrains.dokka.base.renderers.html.templateCommand
import org.jetbrains.dokka.base.renderers.html.templateCommandAsHtmlComment
import org.jetbrains.dokka.base.templating.PathToRootSubstitutionCommand
import org.junit.jupiter.api.io.TempDir
import utils.assertHtmlEqualsIgnoringWhitespace
import java.io.File
import kotlin.test.Test

class SubstitutionCommandResolutionTest : TemplatingAbstractTest() {

    @Test
    fun `should handle PathToRootCommand`(@TempDir outputDirectory: File) {
        val template = createHTML()
            .templateCommand(PathToRootSubstitutionCommand(pattern = "###", default = "default")) {
                a {
                    href = "###index.html"
                    div {
                        id = "logo"
                    }
                }
            }

        val expected = createHTML().a {
            href = "../index.html"
            div {
                id = "logo"
            }
        }
        checkSubstitutedResult(outputDirectory, template, expected)
    }

    @Test
    fun `should handle PathToRootCommand as HTML comment`(@TempDir outputDirectory: File) {
        val template = createHTML().span {
            templateCommandAsHtmlComment(PathToRootSubstitutionCommand(pattern = "###", default = "default")) {
                this@span.a {
                    href = "###index.html"
                    div {
                        id = "logo"
                    }
                }
                templateCommandAsHtmlComment(PathToRootSubstitutionCommand(pattern = "####", default = "default")) {
                    this@span.a {
                        href = "####index.html"
                        div {
                            id = "logo"
                        }
                    }
                }
            }
        }

        val expected = createHTML().span {
            a {
                href = "../index.html"
                div {
                    id = "logo"
                }
            }
            a {
                href = "../index.html"
                div {
                    id = "logo"
                }
            }
        }
        checkSubstitutedResult(outputDirectory, template, expected)
    }

    private fun checkSubstitutedResult(outputDirectory: File, template: String, expected:String) {
        val testedFile = createDirectoriesAndWriteContent(outputDirectory, template)

        val configuration = dokkaConfiguration {
            modules = listOf(
                DokkaModuleDescriptionImpl(
                    name = "module1",
                    relativePathToOutputDirectory = outputDirectory.resolve("module1"),
                    includes = emptySet(),
                    sourceOutputDirectory = outputDirectory.resolve("module1"),
                )
            )
            this.outputDir = outputDirectory
        }

        testFromData(configuration, useOutputLocationFromConfig = true){
            finishProcessingSubmodules = {
                assertHtmlEqualsIgnoringWhitespace(expected, testedFile.readText())
            }
        }
    }

    private fun createDirectoriesAndWriteContent(outputDirectory: File, content: String): File {
        val module1 = outputDirectory.resolve("module1").also { it.mkdirs() }
        val module1Content = module1.resolve("index.html")
        module1Content.writeText(content)
        return module1Content
    }
}
