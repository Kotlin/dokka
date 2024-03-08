/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.util.GradleVersion
import java.io.File
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.test.assertTrue

abstract class AbstractGradleCachingIntegrationTest : AbstractGradleIntegrationTest() {

    /**
     * Create a duplicate [AbstractGradleIntegrationTest.templateProjectDir] in [destination], for Gradle Build Cache
     * testing.
     */
    fun duplicateTemplateProject(buildVersions: BuildVersions, destination: File) {
        prepareProjectFiles(destination = destination)

        updateCustomResourcesPath(destination)

        updateBuildCacheConfig(buildVersions, destination)
    }

    private fun updateCustomResourcesPath(destination: File) {
        // share the same customResources in all projects, because Dokka requires the absolute path of custom resources.
        // (So... Dokka build cache isn't technically relocatable! https://github.com/Kotlin/dokka/issues/2978)
        destination.resolve("customResources").deleteRecursively()
        val originalCustomResources = templateProjectDir.resolve("customResources").invariantSeparatorsPathString

        destination.resolve("build.gradle.kts").apply {
            writeText(
                readText().replace(
                    "val customResourcesDir = layout.projectDirectory.dir(\"customResources\")",
                    "val customResourcesDir = layout.projectDirectory.dir(\"$originalCustomResources\")"
                )
            )
        }
    }

    private fun updateBuildCacheConfig(buildVersions: BuildVersions, destination: File) {
        // share the same build cache in all projects
        val buildCacheDir = tempFolder.resolve("build-cache")

        //Gradle 7.0 removed the old syntax
        val buildCacheLocal =
            if (buildVersions.gradleVersion >= GradleVersion.version("7.0")) "local" else "local<DirectoryBuildCache>"

        destination.resolve("settings.gradle.kts").appendText(
            """
                buildCache {
                    $buildCacheLocal {
                        // Set local build cache directory.
                        directory = File("${buildCacheDir.invariantSeparatorsPath}")
                    }
                }
            """.trimIndent()
        )
    }

    fun File.assertHtmlOutputDir() {
        assertTrue(isDirectory, "Missing dokka html output directory")

        val imagesDir = File(this, "images")
        assertTrue(imagesDir.isDirectory, "Missing images directory")

        val scriptsDir = File(this, "scripts")
        assertTrue(scriptsDir.isDirectory, "Missing scripts directory")
        val reactFile = File(this, "scripts/main.js")
        assertTrue(reactFile.isFile, "Missing main.js")

        val stylesDir = File(this, "styles")
        assertTrue(stylesDir.isDirectory, "Missing styles directory")
        val reactStyles = File(this, "styles/main.css")
        assertTrue(reactStyles.isFile, "Missing main.css")

        val navigationHtml = File(this, "navigation.html")
        assertTrue(navigationHtml.isFile, "Missing navigation.html")

        val moduleOutputDir = File(this, "-basic -project")
        assertTrue(moduleOutputDir.isDirectory, "Missing module directory")

        val moduleIndexHtml = File(this, "index.html")
        assertTrue(moduleIndexHtml.isFile, "Missing module index.html")

        val modulePackageDir = File(moduleOutputDir, "it.basic")
        assertTrue(modulePackageDir.isDirectory, "Missing it.basic package directory")

        val modulePackageIndexHtml = File(modulePackageDir, "index.html")
        assertTrue(modulePackageIndexHtml.isFile, "Missing module package index.html")

        val moduleJavaPackageDir = File(moduleOutputDir, "it.basic.java")
        assertTrue(moduleJavaPackageDir.isDirectory, "Missing it.basic.java package directory")

        allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoSuppressedMarker(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }

        assertTrue(
            allHtmlFiles().any { file -> "Basic Project" in file.readText() },
            "Expected configured moduleName to be present in html"
        )

        assertTrue(
            allHtmlFiles().any { file ->
                "https://github.com/Kotlin/dokka/tree/master/" +
                        "dokka-integration-tests/gradle/projects/it-basic/" +
                        "src/main/kotlin/it/basic/PublicClass.kt" in file.readText()
            },
            "Expected `PublicClass` source link to GitHub"
        )

        assertTrue(
            allHtmlFiles().any { file ->
                "https://github.com/Kotlin/dokka/tree/master/" +
                        "dokka-integration-tests/gradle/projects/it-basic/" +
                        "src/main/java/it/basic/java/SampleJavaClass.java" in file.readText()
            },
            "Expected `SampleJavaClass` source link to GitHub"
        )

        val anchorsShouldNotHaveHashes = "<a data-name=\".*#.*\"\\sanchor-label=\"*.*\">".toRegex()
        assertTrue(
            allHtmlFiles().all { file ->
                !anchorsShouldNotHaveHashes.containsMatchIn(file.readText())
            },
            "Anchors should not have hashes inside"
        )

        assertTrue(
            stylesDir.resolve("logo-styles.css").readText().contains(
                "--dokka-logo-image-url: url('https://upload.wikimedia.org/wikipedia/commons/9/9d/Ubuntu_logo.svg');",
            )
        )
        assertTrue(stylesDir.resolve("custom-style-to-add.css").isFile)
        assertTrue(stylesDir.resolve("custom-style-to-add.css").readText().contains("/* custom stylesheet */"))
        allHtmlFiles().forEach { file ->
            if (file.name != "navigation.html")
                assertTrue(
                    "custom-style-to-add.css" in file.readText(),
                    "custom styles not added to html file ${file.name}"
                )
        }
        assertTrue(imagesDir.resolve("custom-resource.svg").isFile)
    }
}
