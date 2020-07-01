package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.*

class BasicTest : AbstractDefaultVersionsGradleIntegrationTest() {

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-basic")

        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }

        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
    }

    override fun execute(versions: BuildVersions) {
        val result = createGradleRunner(
            buildVersions = versions,
            arguments = arrayOf("dokka", "--stacktrace")
        ).build()
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokka")).outcome)

        val dokkaOutputDir = File(projectDir, "build/dokka")
        assertTrue(dokkaOutputDir.isDirectory, "Missing dokka output directory")

        val imagesDir = File(dokkaOutputDir, "images")
        assertTrue(imagesDir.isDirectory, "Missing images directory")

        val scriptsDir = File(dokkaOutputDir, "scripts")
        assertTrue(scriptsDir.isDirectory, "Missing scripts directory")

        val stylesDir = File(dokkaOutputDir, "styles")
        assertTrue(stylesDir.isDirectory, "Missing styles directory")

        val navigationHtml = File(dokkaOutputDir, "navigation.html")
        assertTrue(navigationHtml.isFile, "Missing navigation.html")

        val moduleOutputDir = File(dokkaOutputDir, "it-basic")
        assertTrue(moduleOutputDir.isDirectory, "Missing module directory")

        val moduleIndexHtml = File(moduleOutputDir, "index.html")
        assertTrue(moduleIndexHtml.isFile, "Missing module index.html")

        val modulePackageDir = File(moduleOutputDir, "it.basic")
        assertTrue(modulePackageDir.isDirectory, "Missing module package directory")

        val modulePackageIndexHtml = File(modulePackageDir, "index.html")
        assertTrue(modulePackageIndexHtml.isFile, "Missing module package index.html")

        dokkaOutputDir.walkTopDown()
            .filter { file -> file.extension == "html" }
            .forEach { file ->
                assertContainsNoErrorClass(file)
                assertNoUnresolvedLInks(file)
            }
    }
}
