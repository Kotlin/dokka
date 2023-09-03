/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.templates

import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import org.jetbrains.dokka.base.renderers.html.SearchRecord
import org.jetbrains.dokka.base.templating.AddToSearch
import org.jetbrains.dokka.base.templating.parseJson
import org.jetbrains.dokka.base.templating.toJsonString
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import kotlin.test.assertEquals

class AddToSearchCommandResolutionTest : TemplatingAbstractTest() {

    @ParameterizedTest
    @ValueSource(strings = ["pages.json"])
    fun `should merge navigation templates`(fileName: String, @TempDir outputDirectory: File) {
        setupTestDirectoriesWithContent(outputDirectory, fileName)

        val configuration = dokkaConfiguration {
            modules = listOf(
                DokkaModuleDescriptionImpl(
                    name = "module1",
                    relativePathToOutputDirectory = outputDirectory.resolve("module1"),
                    includes = emptySet(),
                    sourceOutputDirectory = outputDirectory.resolve("module1"),
                ),
                DokkaModuleDescriptionImpl(
                    name = "module2",
                    relativePathToOutputDirectory = outputDirectory.resolve("module2"),
                    includes = emptySet(),
                    sourceOutputDirectory = outputDirectory.resolve("module2"),
                ),
            )
            this.outputDir = outputDirectory
        }

        testFromData(configuration, useOutputLocationFromConfig = true) {
            finishProcessingSubmodules = { _ ->
                val expected = elements.map { it.copy(location = "module1/${it.location}") } +
                        elements.map { it.copy(location = "module2/${it.location}") }

                val output =
                    parseJson<List<SearchRecord>>(outputDirectory.resolve("scripts/${fileName}").readText())
                assertEquals(expected, output.sortedBy { it.location })
            }
        }
    }

    private fun setupTestDirectoriesWithContent(outputDirectory: File, fileName: String): List<File> {
        val scriptsForModule1 = outputDirectory.resolve("module1/scripts").also { it.mkdirs() }
        val scriptsForModule2 = outputDirectory.resolve("module2/scripts").also { it.mkdirs() }
        outputDirectory.resolve("scripts").also { it.mkdirs() }

        val module1Navigation = scriptsForModule1.resolve(fileName)
        module1Navigation.writeText(toJsonString(fromModule1))
        val module2Navigation = scriptsForModule2.resolve(fileName)
        module2Navigation.writeText(toJsonString(fromModule2))

        return listOf(module1Navigation, module2Navigation)
    }

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
}
