package org.jetbrains.dokka.templates

import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import org.jetbrains.dokka.base.renderers.html.templateCommand
import org.jetbrains.dokka.base.templating.PathToRootSubstitutionCommand
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.jupiter.api.Test
import utils.assertHtmlEqualsIgnoringWhitespace
import java.io.File

class SubstitutionCommandResolutionTest : TemplatingAbstractTest() {

    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `should handle PathToRootCommand`() {
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

        val testedFile = createDirectoriesAndWriteContent(template)

        val configuration = dokkaConfiguration {
            modules = listOf(
                DokkaModuleDescriptionImpl(
                    name = "module1",
                    relativePathToOutputDirectory = folder.root.resolve("module1"),
                    includes = emptySet(),
                    sourceOutputDirectory = folder.root.resolve("module1"),
                )
            )
            this.outputDir = folder.root
        }

        testFromData(configuration, preserveOutputLocation = true){
            finishProcessingSubmodules = {
                assertHtmlEqualsIgnoringWhitespace(expected, testedFile.readText())
            }
        }
    }

    private fun createDirectoriesAndWriteContent(content: String): File {
        folder.create()
        val module1 = folder.newFolder("module1")
        val module1Content = module1.resolve("index.html")
        module1Content.writeText(content)
        return module1Content
    }

}
