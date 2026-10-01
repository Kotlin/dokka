/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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
import org.junit.jupiter.api.io.TempDir
import utils.assertHtmlEqualsIgnoringWhitespace
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ResolveLinkCommandResolutionTest : MultiModuleAbstractTest() {

    @Test
    fun `should resolve link to another module`(@TempDir outputDirectory: File) {
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

        val contentFile = setup(outputDirectory, link)
        // The page the link points to must actually exist, otherwise it is treated as a
        // broken link to a symbol excluded from the documentation (see #4448).
        outputDirectory.resolve("module2/package2/-sample/index.html")
            .also { assertTrue(it.parentFile.mkdirs()) }
            .writeText("<html></html>")
        val configuration = createConfiguration(outputDirectory)

        testFromData(configuration, useOutputLocationFromConfig = true) {
            finishProcessingSubmodules = {
                assertHtmlEqualsIgnoringWhitespace(expected, contentFile.readText())
            }
        }
    }

    @Test
    fun `should not resolve link to a symbol excluded from the documentation`(@TempDir outputDirectory: File) {
        // The package is documented (present in the package-list), but the symbol itself is
        // excluded, so its page is never generated. The link must not be rendered as a broken
        // link to a non-existent page (#4448).
        val testedDri = DRI(
            packageName = "package2",
            classNames = "Excluded",
        )
        val link = createHTML().templateCommand(ResolveLinkCommand(testedDri)) {
            span {
                +"Excluded"
            }
        }

        val expected = createHTML().span {
            attributes["data-unresolved-link"] = testedDri.toString()
            span {
                +"Excluded"
            }
        }

        val contentFile = setup(outputDirectory, link)
        // Note: no page file is created for `package2/-excluded`, mimicking an excluded symbol.
        val configuration = createConfiguration(outputDirectory)

        testFromData(configuration, useOutputLocationFromConfig = true) {
            finishProcessingSubmodules = {
                assertHtmlEqualsIgnoringWhitespace(expected, contentFile.readText())
            }
        }
    }

    @Test
    fun `should produce content when link is not resolvable`(@TempDir outputDirectory: File) {
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

        val contentFile = setup(outputDirectory, link)
        val configuration = createConfiguration(outputDirectory)

        testFromData(configuration, useOutputLocationFromConfig = true) {
            finishProcessingSubmodules = {
                assertHtmlEqualsIgnoringWhitespace(expected, contentFile.readText())
            }
        }
    }

    private fun setup(outputDirectory: File, content: String): File {
        val innerModule1 = outputDirectory.resolve("module1").also { assertTrue(it.mkdirs()) }
        val innerModule2 = outputDirectory.resolve("module2").also { assertTrue(it.mkdirs()) }
        val packageList = innerModule2.resolve("package-list")
        packageList.writeText(mockedPackageListForPackages(RecognizedLinkFormat.DokkaHtml, "package2"))
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
