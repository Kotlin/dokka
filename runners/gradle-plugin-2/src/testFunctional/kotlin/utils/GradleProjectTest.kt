package org.jetbrains.dokka.gradle.utils

import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


// utils for testing using Gradle TestKit


class GradleProjectTest(
    val projectDir: Path,
    val runner: GradleRunner,
) {

    val testMavenRepoDir: String = System.getProperty("testMavenRepoDir")

    fun createFile(filePath: String, contents: String): File =
        projectDir.resolve(filePath).toFile().apply {
            parentFile.mkdirs()
            createNewFile()
            writeText(contents)
        }
}

private class TestProjectFile(
    val filePath: String,
) : ReadWriteProperty<GradleProjectTest, String> {
    override fun getValue(thisRef: GradleProjectTest, property: KProperty<*>): String =
        thisRef.projectDir.resolve(filePath).toFile().readText()

    override fun setValue(thisRef: GradleProjectTest, property: KProperty<*>, value: String) {
        thisRef.createFile(filePath, value)
    }
}

@get:Language("kts")
@set:Language("kts")
var GradleProjectTest.settingsGradleKts: String by TestProjectFile("settings.gradle.kts")


@get:Language("kts")
@set:Language("kts")
var GradleProjectTest.buildGradleKts: String by TestProjectFile("build.gradle.kts")


@get:Language("groovy")
@set:Language("groovy")
var GradleProjectTest.settingsGradle: String by TestProjectFile("settings.gradle")


@get:Language("groovy")
@set:Language("groovy")
var GradleProjectTest.buildGradle: String by TestProjectFile("build.gradle")


@get:Language("properties")
@set:Language("properties")
var GradleProjectTest.gradleProperties: String by TestProjectFile("gradle.properties")

/** Builder for testing a Gradle project that uses Kotlin script DSL */
fun gradleKtsProjectTest(
    projectDir: Path = Files.createTempDirectory("dokka-test"),
    runner: GradleRunner = GradleRunner.create().withProjectDir(projectDir.toFile()),
    build: GradleProjectTest.() -> Unit,
): GradleProjectTest {
    return GradleProjectTest(projectDir, runner).apply {

        settingsGradleKts = """
            rootProject.name = "test"
            
            @Suppress("UnstableApiUsage") // Central declaration of repositories is an incubating feature
            dependencyResolutionManagement {
            
                repositories {
                    mavenCentral()
                    maven(file("$testMavenRepoDir"))
                }
            
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                        mavenCentral()
                        maven(file("$testMavenRepoDir"))
                    }
                }
            }

        """.trimIndent()

        gradleProperties = """
            kotlin.mpp.stability.nowarn=true
            org.gradle.cache=true
       """.trimIndent()

        build()
    }
}
