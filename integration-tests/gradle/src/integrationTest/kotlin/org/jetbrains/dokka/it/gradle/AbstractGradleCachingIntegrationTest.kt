package org.jetbrains.dokka.it.gradle

import org.gradle.util.GradleVersion
import java.io.File
import kotlin.test.*

abstract class AbstractGradleCachingIntegrationTest(override val versions: BuildVersions): AbstractGradleIntegrationTest() {
    fun setupProject(project: File) {
        val templateProjectDir = File("projects", "it-basic")
        project.mkdirs()
        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(project, topLevelFile.name)) }

        File(templateProjectDir, "src").copyRecursively(File(project, "src"))
        val customResourcesDir = File(templateProjectDir, "customResources")
        if(customResourcesDir.exists() && customResourcesDir.isDirectory) {
            val destination = File(project.parentFile, "customResources")
            destination.mkdirs()
            destination.deleteRecursively()
            customResourcesDir.copyRecursively(destination)
        }

        // clean local cache for each test
        if (versions.gradleVersion >= GradleVersion.version("7.0")) {
            //Gradle 7.0 removed the old syntax
            project.toPath().resolve("settings.gradle.kts").toFile().appendText(
                """
                buildCache {
                    local {
                        // Set local build cache directory.
                        directory = File("${projectDir.absolutePath}", "build-cache")
                    }
                }
            """.trimIndent()
            )
        } else {
            project.toPath().resolve("settings.gradle.kts").toFile().appendText(
                """
                buildCache {
                    local<DirectoryBuildCache> {
                        // Set local build cache directory.
                        directory = File("${projectDir.absolutePath}", "build-cache")
                    }
                }
            """.trimIndent()
            )
        }
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
                        "integration-tests/gradle/projects/it-basic/" +
                        "src/main/kotlin/it/basic/PublicClass.kt" in file.readText()
            },
            "Expected `PublicClass` source link to GitHub"
        )

        assertTrue(
            allHtmlFiles().any { file ->
                "https://github.com/Kotlin/dokka/tree/master/" +
                        "integration-tests/gradle/projects/it-basic/" +
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

        assertEquals(
            """#logo{background-image:url('https://upload.wikimedia.org/wikipedia/commons/9/9d/Ubuntu_logo.svg');}""",
            stylesDir.resolve("logo-styles.css").readText().replace("\\s".toRegex(), ""),
        )
        assertTrue(stylesDir.resolve("custom-style-to-add.css").isFile)
        assertEquals("""/* custom stylesheet */""", stylesDir.resolve("custom-style-to-add.css").readText())
        allHtmlFiles().forEach { file ->
            if(file.name != "navigation.html") assertTrue("custom-style-to-add.css" in file.readText(), "custom styles not added to html file ${file.name}")
        }
        assertTrue(imagesDir.resolve("custom-resource.svg").isFile)
    }
}