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
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.rules.TemporaryFolder
import utils.assertHtmlEqualsIgnoringWhitespace

class AddToNavigationCommandResolutionTest : TemplatingAbstractTest() {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `should substitute AddToNavigationCommand in root directory`() =
        addToNavigationTest {
            val output = folder.root.resolve("navigation.html").readText()
            val expected = expectedOutput(
                ModuleWithPrefix("module1"),
                ModuleWithPrefix("module2")
            )
            assertHtmlEqualsIgnoringWhitespace(expected, output)
        }

    @ParameterizedTest
    @ValueSource(strings = ["module1", "module2"])
    fun `should substitute AddToNavigationCommand in modules directory`(moduleName: String) =
        addToNavigationTest {
            val output = folder.root.resolve(moduleName).resolve("navigation.html").readText()
            val expected = expectedOutput(
                ModuleWithPrefix("module1", ".."),
                ModuleWithPrefix("module2", "..")
            )
            assertHtmlEqualsIgnoringWhitespace(expected, output)
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

    private fun addToNavigationTest(test: (DokkaContext) -> Unit) {
        folder.create()
        val module1 = folder.newFolder("module1")
        val module2 = folder.newFolder("module2")

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
            this.outputDir = folder.root
        }

        val module1Navigation = module1.resolve("navigation.html")
        module1Navigation.writeText(inputForModule("module1"))
        val module2Navigation = module2.resolve("navigation.html")
        module2Navigation.writeText(inputForModule("module2"))

        testFromData(configuration, preserveOutputLocation = true) {
            finishProcessingSubmodules = { ctx ->
                test(ctx)
            }
        }
    }

    private data class ModuleWithPrefix(val moduleName: String, val prefix: String? = null)
}
