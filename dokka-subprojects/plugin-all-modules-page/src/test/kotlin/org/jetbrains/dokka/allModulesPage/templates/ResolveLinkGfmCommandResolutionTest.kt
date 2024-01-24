/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.allModulesPage.templates

import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import org.jetbrains.dokka.allModulesPage.MultiModuleAbstractTest
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.gfm.GfmCommand.Companion.templateCommand
import org.jetbrains.dokka.gfm.GfmPlugin
import org.jetbrains.dokka.gfm.ResolveLinkGfmCommand
import org.jetbrains.dokka.gfm.templateProcessing.GfmTemplateProcessingPlugin
import org.jetbrains.dokka.links.DRI
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResolveLinkGfmCommandResolutionTest : MultiModuleAbstractTest() {

    @Test
    fun `should resolve link to another module`(@TempDir outputDirectory: File) {
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
                )
            )
            outputDir = outputDirectory
        }

        val innerModule1 = outputDirectory.resolve("module1").also { assertTrue(it.mkdirs()) }
        val innerModule2 = outputDirectory.resolve("module2").also { assertTrue(it.mkdirs()) }

        val indexMd = innerModule1.resolve("index.md")
        val packageList = innerModule2.resolve("package-list")

        val indexMdContent = StringBuilder().apply {
            templateCommand(
                ResolveLinkGfmCommand(
                    dri = DRI(
                        packageName = "package2",
                        classNames = "Sample",
                    )
                )
            ) {
                append("Sample text inside")
            }
        }.toString()

        indexMd.writeText(indexMdContent)
        packageList.writeText(mockedPackageListForPackages(RecognizedLinkFormat.DokkaGFM, "package2"))

        testFromData(
            configuration,
            pluginOverrides = listOf(GfmTemplateProcessingPlugin(), GfmPlugin()),
            useOutputLocationFromConfig = true
        ) {
            finishProcessingSubmodules = {
                val expectedIndexMd = "[Sample text inside](../module2/package2/-sample/index.md)"
                assertEquals(expectedIndexMd, indexMd.readText().trim())
            }
        }
    }
}
