package org.jetbrains.dokka.allModulesPage.templates

import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import org.jetbrains.dokka.allModulesPage.MultiModuleAbstractTest
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.gfm.GfmCommand.Companion.templateCommand
import org.jetbrains.dokka.gfm.GfmPlugin
import org.jetbrains.dokka.gfm.ResolveLinkGfmCommand
import org.jetbrains.dokka.gfm.templateProcessing.GfmTemplateProcessingPlugin
import org.jetbrains.dokka.links.DRI
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

class ResolveLinkGfmCommandResolutionTest : MultiModuleAbstractTest() {
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
        outputDir = folder.root
    }

    @Test
    fun `should resolve link to another module`(){
        val testedDri = DRI(
            packageName = "package2",
            classNames = "Sample",
        )

        val link = StringBuilder().apply {
            templateCommand(ResolveLinkGfmCommand(testedDri)){
                append("Sample text inside")
            }
        }.toString()

        val expected = "[Sample text inside](../module2/package2/-sample/index.md)"

        val content = setup(link)
        val configuration = configuration()

        testFromData(configuration, pluginOverrides = listOf(GfmTemplateProcessingPlugin(), GfmPlugin()), preserveOutputLocation = true) {
            finishProcessingSubmodules = {
                assertEquals(expected, content.readText().trim())
            }
        }
    }

    private fun setup(content: String): File {
        folder.create()
        val innerModule1 = folder.newFolder( "module1")
        val innerModule2 = folder.newFolder( "module2")
        val packageList = innerModule2.resolve("package-list")
        packageList.writeText(mockedPackageListForPackages(RecognizedLinkFormat.DokkaGFM, "package2"))
        val contentFile = innerModule1.resolve("index.md")
        contentFile.writeText(content)
        return contentFile
    }
}
