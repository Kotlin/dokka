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
import org.jetbrains.dokka.base.templating.AddToNavigationCommand
import org.jetbrains.dokka.plugability.DokkaContext
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import utils.assertHtmlEqualsIgnoringWhitespace
import java.io.File
import kotlin.test.Test

class AddToNavigationCommandResolutionTest : TemplatingAbstractTest() {

    @Test
    fun `should substitute AddToNavigationCommand in root directory`(@TempDir outputDirectory: File) {
        addToNavigationTest(outputDirectory) {
            val output = outputDirectory.resolve("navigation.html").readText()
            val expected = expectedOutput(
                ModuleWithPrefix("module1"),
                ModuleWithPrefix("module2")
            )
            assertHtmlEqualsIgnoringWhitespace(expected, output)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["module1", "module2"])
    fun `should substitute AddToNavigationCommand in modules directory`(
        moduleName: String,
        @TempDir outputDirectory: File
    ) {
        addToNavigationTest(outputDirectory) {
            val output = outputDirectory.resolve(moduleName).resolve("navigation.html").readText()
            val expected = expectedOutput(
                ModuleWithPrefix("module1", ".."),
                ModuleWithPrefix("module2", "..")
            )
            assertHtmlEqualsIgnoringWhitespace(expected, output)
        }
    }

    private fun expectedOutput(vararg modulesWithPrefix: ModuleWithPrefix) = createHTML(prettyPrint = true)
        .div("sideMenu") {
            modulesWithPrefix.forEach { (moduleName, prefix) ->
                val relativePrefix = prefix?.let { "$it/" } ?: ""
                div("sideMenuPart") {
                    id = "$moduleName-nav-submenu"
                    div("overview") {
                        a {
                            href = "$relativePrefix$moduleName/module-page.html"
                            span {
                                +"module-$moduleName"
                            }
                        }
                    }
                    div("sideMenuPart") {
                        id = "$moduleName-nav-submenu-0"
                        div("overview") {
                            a {
                                href = "$relativePrefix$moduleName/$moduleName/package-page.html"
                                span {
                                    +"package-$moduleName"
                                }
                            }
                        }
                    }
                }
            }
        }

    private fun inputForModule(moduleName: String) = createHTML()
        .templateCommand(AddToNavigationCommand(moduleName)) {
            div("sideMenuPart") {
                id = "$moduleName-nav-submenu"
                div("overview") {
                    a {
                        href = "module-page.html"
                        span {
                            +"module-$moduleName"
                        }
                    }
                }
                div("sideMenuPart") {
                    id = "$moduleName-nav-submenu-0"
                    div("overview") {
                        a {
                            href = "$moduleName/package-page.html"
                            span {
                                +"package-$moduleName"
                            }
                        }
                    }
                }
            }
        }

    private fun addToNavigationTest(outputDirectory: File, test: (DokkaContext) -> Unit) {
        val module1 = outputDirectory.resolve("module1").also { it.mkdirs() }
        val module2 = outputDirectory.resolve("module2").also { it.mkdirs() }

        val configuration = dokkaConfiguration {
            modules = listOf(
                DokkaModuleDescriptionImpl(
                    name = "module1",
                    relativePathToOutputDirectory = module1,
                    includes = emptySet(),
                    sourceOutputDirectory = module1,
                ),
                DokkaModuleDescriptionImpl(
                    name = "module2",
                    relativePathToOutputDirectory = module2,
                    includes = emptySet(),
                    sourceOutputDirectory = module2,
                ),
            )
            this.outputDir = outputDirectory
        }

        val module1Navigation = module1.resolve("navigation.html")
        module1Navigation.writeText(inputForModule("module1"))
        val module2Navigation = module2.resolve("navigation.html")
        module2Navigation.writeText(inputForModule("module2"))

        testFromData(configuration, useOutputLocationFromConfig = true) {
            finishProcessingSubmodules = { ctx ->
                test(ctx)
            }
        }
    }

    private data class ModuleWithPrefix(val moduleName: String, val prefix: String? = null)
}
