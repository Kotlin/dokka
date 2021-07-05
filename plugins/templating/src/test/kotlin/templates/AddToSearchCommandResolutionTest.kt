package org.jetbrains.dokka.templates

import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import org.jetbrains.dokka.base.renderers.html.SearchRecord
import org.jetbrains.dokka.base.templating.AddToSearch
import org.jetbrains.dokka.base.templating.parseJson
import org.jetbrains.dokka.base.templating.toJsonString
import org.junit.Rule
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

class AddToSearchCommandResolutionTest : TemplatingAbstractTest() {
    companion object {
        val elements = listOf(
            SearchRecord(name = "name1", location = "location1"),
            SearchRecord(name = "name2", location = "location2")
        )
        val fromModule1 = AddToSearch(
            moduleName = "module1",
            elements = elements
        )
        val fromModule2 = AddToSearch(
            moduleName = "module2",
            elements = elements
        )
    }

    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    @ParameterizedTest
    @ValueSource(strings = ["navigation-pane.json", "pages.json"])
    fun `should merge navigation templates`(fileName: String) {
        val (module1Navigation, module2Navigation) = setupTestDirectoriesWithContent(fileName)

        val outputDir = folder.root
        val configuration = dokkaConfiguration {
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
                ),
            )
            this.outputDir = outputDir
        }

        testFromData(configuration, preserveOutputLocation = true) {
            finishProcessingSubmodules = { _ ->
                val expected = elements.map { it.copy(location = "module1/${it.location}") } +
                        elements.map { it.copy(location = "module2/${it.location}") }

                val output =
                    parseJson<List<SearchRecord>>(outputDir.resolve("scripts/${fileName}").readText())
                assertEquals(expected, output.sortedBy { it.location })
            }
        }
    }

    private fun setupTestDirectoriesWithContent(fileName: String): List<File> {
        folder.create()
        val scriptsForModule1 = folder.newFolder("module1", "scripts")
        val scriptsForModule2 = folder.newFolder("module2", "scripts")
        folder.newFolder("scripts")

        val module1Navigation = scriptsForModule1.resolve(fileName)
        module1Navigation.writeText(toJsonString(fromModule1))
        val module2Navigation = scriptsForModule2.resolve(fileName)
        module2Navigation.writeText(toJsonString(fromModule2))

        return listOf(module1Navigation, module2Navigation)
    }
}
