/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.allModulesPage.templates

import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.span
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import org.jetbrains.dokka.allModulesPage.MultiModuleAbstractTest
import org.jetbrains.dokka.base.renderers.html.templateCommand
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.base.templating.ResolveLinkCommand
import org.jetbrains.dokka.links.DRI
import org.junit.jupiter.api.io.TempDir
import utils.assertHtmlEqualsIgnoringWhitespace
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The test checks the first case (short-term solution) of #3368:
 * a situation where 2 (or more) local modules have the same package name.
 *
 * It does not check links for external modules, only for local ones.
 */
class ResolveLinkOfTheSamePackagesInDifferentModulesCommandResolutionTest : MultiModuleAbstractTest() {

    @Test
    fun `should resolve link to the same package from two different modules`(@TempDir outputDirectory: File) {
        val testedDriInFirstModule = DRI(
            packageName = "package2",
            classNames = "Sample",
        )

        val testedDriInSecondModule = DRI(
            packageName = "package2",
            classNames = "Sample2",
        )
        val link = createHTML().div {
            templateCommand(ResolveLinkCommand(testedDriInFirstModule)) {
                span {
                    +"Sample"
                }
            }
            templateCommand(ResolveLinkCommand(testedDriInSecondModule)) {
                span {
                    +"Sample2"
                }
            }
        }

        val expected = createHTML().div {
            a {
                href = "../module1/package2/-sample/index.html"
                span {
                    +"Sample"
                }
            }
            a {
                href = "../module2/package2/-sample2/index.html"
                span {
                    +"Sample2"
                }
            }
        }


        val contentFile = setup(
            outputDirectory = outputDirectory,
            content = link,
            resolutionTargetInFirstModule = testedDriInFirstModule,
            resolutionTargetInSecondModule = testedDriInSecondModule
        )
        val configuration = createConfiguration(outputDirectory)

        testFromData(configuration, useOutputLocationFromConfig = true) {
            finishProcessingSubmodules = {
                assertHtmlEqualsIgnoringWhitespace(expected, contentFile.readText())
            }
        }
    }

    /**
     * Create partial output for two modules: `module1` and `module2`.
     * The modules have the same package name `package2` in `package-list`s.
     */
    private fun setup(
        outputDirectory: File,
        content: String,
        resolutionTargetInFirstModule: DRI? = null,
        resolutionTargetInSecondModule: DRI? = null
    ): File {
        val innerModule1 = outputDirectory.resolve("module1").also { assertTrue(it.mkdirs()) }
        val innerModule2 = outputDirectory.resolve("module2").also { assertTrue(it.mkdirs()) }
        val packageList2 = innerModule2.resolve("package-list")
        packageList2.writeText(mockedPackageListForPackages(RecognizedLinkFormat.DokkaHtml, "package2"))
        val packageList1 = innerModule1.resolve("package-list")
        packageList1.writeText(mockedPackageListForPackages(RecognizedLinkFormat.DokkaHtml, "package2"))

        if (resolutionTargetInFirstModule != null) {
            val resolvedFile1 = innerModule1
                .resolve("${resolutionTargetInFirstModule.packageName}/-${resolutionTargetInFirstModule.classNames?.toLowerCase()}/index.html")
            resolvedFile1.parentFile.mkdirs()
            resolvedFile1.createNewFile()
        }
        if (resolutionTargetInSecondModule != null) {
            val resolvedFile2 = innerModule2
                .resolve("${resolutionTargetInSecondModule.packageName}/-${resolutionTargetInSecondModule.classNames?.toLowerCase()}/index.html")
            resolvedFile2.parentFile.mkdirs()
            resolvedFile2.createNewFile()
        }

        val contentFile = innerModule1.resolve("index.html")
        contentFile.writeText(content)
        return contentFile
    }

    private fun createConfiguration(outputDirectory: File) = dokkaConfiguration {
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
        this.outputDir = outputDirectory
    }
}
