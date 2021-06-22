package org.jetbrains.dokka.allModulesPage.templates

import kotlinx.html.a
import kotlinx.html.span
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import org.jetbrains.dokka.allModulesPage.MultiModuleAbstractTest
import org.jetbrains.dokka.base.renderers.html.templateCommand
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.base.templating.ResolveLinkCommand
import org.jetbrains.dokka.links.DRI
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.rules.TemporaryFolder
import utils.assertHtmlEqualsIgnoringWhitespace
import java.io.File

class ResolveLinkCommandResolutionTest : MultiModuleAbstractTest() {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private fun configuration() = dokkaConfiguration {
        modules = listOf(
            DokkaModuleDescriptionImpl(
                name = "module1",
                relativePathToOutputDirectory = folder.root.resolve("module1"),
                includes = emptySet(),
                sourceOutputDirectory = folder.root.resolve("module1"),
            ),
            DokkaModuleDescriptionImpl(
                name = "module2",
                relativePathToOutputDirectory = folder.root.resolve("module2"),
                includes = emptySet(),
                sourceOutputDirectory = folder.root.resolve("module2"),
            )
        )
        this.outputDir = folder.root
    }

    @Test
    fun `should resolve link to another module`() {
        val testedDri = DRI(
            packageName = "package2",
            classNames = "Sample",
        )
        val link = createHTML().templateCommand(ResolveLinkCommand(testedDri)) {
            span {
                +"Sample"
            }
        }

        val expected = createHTML().a {
            href = "../module2/package2/-sample/index.html"
            span {
                +"Sample"
            }
        }

        val contentFile = setup(link)
        val configuration = configuration()

        testFromData(configuration, preserveOutputLocation = true) {
            finishProcessingSubmodules = {
                assertHtmlEqualsIgnoringWhitespace(expected, contentFile.readText())
            }
        }
    }

    @Test
    fun `should produce content when link is not resolvable`() {
        val testedDri = DRI(
            packageName = "not-resolvable-package",
            classNames = "Sample",
        )
        val link = createHTML().templateCommand(ResolveLinkCommand(testedDri)) {
            span {
                +"Sample"
            }
        }

        val expected = createHTML().span {
            attributes["data-unresolved-link"] = testedDri.toString()
            span {
                +"Sample"
            }
        }

        val contentFile = setup(link)
        val configuration = configuration()

        testFromData(configuration, preserveOutputLocation = true) {
            finishProcessingSubmodules = {
                assertHtmlEqualsIgnoringWhitespace(expected, contentFile.readText())
            }
        }
    }

    fun setup(content: String): File {
        folder.create()
        val innerModule1 = folder.newFolder("module1")
        val innerModule2 = folder.newFolder("module2")
        val packageList = innerModule2.resolve("package-list")
        packageList.writeText(mockedPackageListForPackages(RecognizedLinkFormat.DokkaHtml, "package2"))
        val contentFile = innerModule1.resolve("index.html")
        contentFile.writeText(content)
        return contentFile
    }
}
