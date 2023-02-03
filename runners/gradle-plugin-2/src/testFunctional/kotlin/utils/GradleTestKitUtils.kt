package org.jetbrains.dokka.gradle.utils

import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
import kotlin.reflect.KProperty


// utils for testing using Gradle TestKit


class GradleProjectTest(
    name: String,
    val projectDir: Path = tempDir(name),
    val runner: GradleRunner = GradleRunner.create().withProjectDir(projectDir.toFile()),
) {

    val testMavenRepoDir: String = System.getProperty("testMavenRepoDir")

    fun createFile(filePath: String, contents: String): File =
        projectDir.resolve(filePath).toFile().apply {
            parentFile.mkdirs()
            createNewFile()
            writeText(contents)
        }

    companion object {
        fun tempDir(name: String): Path {
            val funcTestTempDir = System.getProperty("funcTestTempDir")

            val safeName = name.map { if (it.isLetterOrDigit()) it else "-" }.joinToString("")

            return if (funcTestTempDir != null) {
                Paths.get(funcTestTempDir, safeName)
            } else {
                Files.createTempDirectory("dokka-test-$safeName")
            }
        }
    }
}

/**
 * Builder for testing a Gradle project that uses Kotlin script DSL and creates default
 * `settings.gradle.kts` and `gradle.properties` files.
 */
fun gradleKtsProjectTest(
    name: String = Random.nextInt(100_000_000, 999_999_999).toString(),
    build: GradleProjectTest.() -> Unit,
): GradleProjectTest {
    return GradleProjectTest(name).apply {

        settingsGradleKts = """
            |rootProject.name = "test"
            |
            |@Suppress("UnstableApiUsage") // Central declaration of repositories is an incubating feature
            |dependencyResolutionManagement {
            |    repositories {
            |        mavenCentral()
            |        maven(file("$testMavenRepoDir"))
            |    }
            |}
            |
            |pluginManagement {
            |    repositories {
            |        gradlePluginPortal()
            |        mavenCentral()
            |        maven(file("$testMavenRepoDir"))
            |    }
            |}
            |
        """.trimMargin()

        gradleProperties = """
            |kotlin.mpp.stability.nowarn=true
            |org.gradle.cache=true
       """.trimMargin()

        build()
    }
}

/**
 * Builder for testing a Gradle project that uses Groovy script and creates default,
 * `settings.gradle`, and `gradle.properties` files.
 */
fun gradleGroovyProjectTest(
    name: String = Random.nextInt(100_000_000, 999_999_999).toString(),
    build: GradleProjectTest.() -> Unit,
): GradleProjectTest {
    return GradleProjectTest(name).apply {

        settingsGradle = """
            |rootProject.name = "test"
            |
            |dependencyResolutionManagement {
            |    repositories {
            |        mavenCentral()
            |        maven { url = file("$testMavenRepoDir") }
            |    }
            |}
            |
            |pluginManagement {
            |    repositories {
            |        gradlePluginPortal()
            |        mavenCentral()
            |        maven { url = file("$testMavenRepoDir") }
            |    }
            |}
            |
        """.trimMargin()

        gradleProperties = """
            |kotlin.mpp.stability.nowarn=true
            |org.gradle.cache=true
       """.trimMargin()

        build()
    }
}


/** Delegate for reading and writing a [GradleProjectTest] file. */
private class TestProjectFile(
    val filePath: String,
) : ReadWriteProperty<GradleProjectTest, String> {
    override fun getValue(thisRef: GradleProjectTest, property: KProperty<*>): String =
        thisRef.projectDir.resolve(filePath).toFile().readText()

    override fun setValue(thisRef: GradleProjectTest, property: KProperty<*>, value: String) {
        thisRef.createFile(filePath, value)
    }
}

/** Set the content of `settings.gradle.kts` */
@delegate:Language("kts")
var GradleProjectTest.settingsGradleKts: String by TestProjectFile("settings.gradle.kts")


/** Set the content of `build.gradle.kts` */
@delegate:Language("kts")
var GradleProjectTest.buildGradleKts: String by TestProjectFile("build.gradle.kts")


/** Set the content of `settings.gradle` */
@delegate:Language("groovy")
var GradleProjectTest.settingsGradle: String by TestProjectFile("settings.gradle")


/** Set the content of `build.gradle` */
@delegate:Language("groovy")
var GradleProjectTest.buildGradle: String by TestProjectFile("build.gradle")


/** Set the content of `gradle.properties` */
@delegate:Language("properties")
var GradleProjectTest.gradleProperties: String by TestProjectFile("gradle.properties")

fun GradleProjectTest.createKotlinFile(filePath: String, @Language("kotlin") contents: String) =
    createFile(filePath, contents)

fun GradleProjectTest.createKtsFile(filePath: String, @Language("kts") contents: String) =
    createFile(filePath, contents)
