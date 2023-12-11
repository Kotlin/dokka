/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import java.io.File.pathSeparator


// utils for testing using Gradle TestKit


class GradleProjectTest(
    override val projectDir: Path,
) : ProjectDirectoryScope {

    constructor(
        testProjectName: String,
        baseDir: Path = funcTestTempDir,
    ) : this(projectDir = baseDir.resolve(testProjectName))

    val runner: GradleRunner
        get() = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
//            .withJvmArguments(
//                "-XX:MaxMetaspaceSize=512m",
//                "-XX:+AlwaysPreTouch", // https://github.com/gradle/gradle/issues/3093#issuecomment-387259298
//            )
            .addArguments()

//    val testMavenRepoRelativePath: String =
//        projectDir.relativize(projectLocalMavenDir).toFile().invariantSeparatorsPath

    companion object {

        /** file-based Maven Repo that contains the Dokka dependencies */
        val projectLocalMavenDirs: List<Path> by systemProperty { it.split(pathSeparator).map(Paths::get) }

        val projectTestTempDir: Path by systemProperty(Paths::get)

        /** Temporary directory for the functional tests */
        val funcTestTempDir: Path by lazy {
            projectTestTempDir.resolve("functional-tests")
        }

//        /** Dokka Source directory that contains Gradle projects used for integration tests */
//        val integrationTestProjectsDir: Path by systemProperty(Paths::get)

//        /** Dokka Source directory that contains example Gradle projects */
//        val exampleProjectsDir: Path by systemProperty(Paths::get)
        val templateProjectsDir: Path by systemProperty(Paths::get)
    }
}


/** Delegate for reading and writing a [GradleProjectTest] file. */
fun GradleProjectTest.projectFile(
    @Language("TEXT")
    filePath: String
): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, String>> =
    PropertyDelegateProvider { _, _ ->
        TestProjectFileProvidedDelegate(this, filePath)
    }


/** Delegate for reading and writing a [GradleProjectTest] file. */
private class TestProjectFileProvidedDelegate(
    private val project: GradleProjectTest,
    private val filePath: String,
) : ReadWriteProperty<Any?, String> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String =
        project.projectDir.resolve(filePath).toFile().readText()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        project.createFile(filePath, value)
    }
}

/** Delegate for reading and writing a [GradleProjectTest] file. */
class TestProjectFileDelegate(
    private val filePath: String,
) : ReadWriteProperty<ProjectDirectoryScope, String> {
    override fun getValue(thisRef: ProjectDirectoryScope, property: KProperty<*>): String =
        thisRef.projectDir.resolve(filePath).toFile().readText()

    override fun setValue(thisRef: ProjectDirectoryScope, property: KProperty<*>, value: String) {
        thisRef.createFile(filePath, value)
    }
}


@DslMarker
annotation class ProjectDirectoryDsl

@ProjectDirectoryDsl
interface ProjectDirectoryScope {
    val projectDir: Path
}

private data class ProjectDirectoryScopeImpl(
    override val projectDir: Path
) : ProjectDirectoryScope


fun ProjectDirectoryScope.createFile(filePath: String, contents: String): File =
    projectDir.resolve(filePath).toFile().apply {
        parentFile.mkdirs()
        createNewFile()
        writeText(contents)
    }


@ProjectDirectoryDsl
fun ProjectDirectoryScope.dir(
    path: String,
    block: ProjectDirectoryScope.() -> Unit = {},
): ProjectDirectoryScope =
    ProjectDirectoryScopeImpl(projectDir.resolve(path)).apply(block)


@ProjectDirectoryDsl
fun ProjectDirectoryScope.file(
    path: String
): Path = projectDir.resolve(path)


fun ProjectDirectoryScope.findFiles(matcher: (File) -> Boolean): Sequence<File> =
    projectDir.toFile().walk().filter(matcher)


/** Set the content of `settings.gradle.kts` */
@delegate:Language("kts")
var ProjectDirectoryScope.settingsGradleKts: String by
TestProjectFileDelegate(/* language=text */ "settings.gradle.kts")


/** Set the content of `build.gradle.kts` */
@delegate:Language("kts")
var ProjectDirectoryScope.buildGradleKts: String by
TestProjectFileDelegate(/* language=text */ "build.gradle.kts")


/** Set the content of `settings.gradle` */
@delegate:Language("groovy")
var ProjectDirectoryScope.settingsGradle: String by
TestProjectFileDelegate(/* language=text */ "settings.gradle")


/** Set the content of `build.gradle` */
@delegate:Language("groovy")
var ProjectDirectoryScope.buildGradle: String by
TestProjectFileDelegate(/* language=text */ "build.gradle")


/** Set the content of `gradle.properties` */
@delegate:Language("properties")
var ProjectDirectoryScope.gradleProperties: String by
TestProjectFileDelegate(/* language=text */ "gradle.properties")


fun ProjectDirectoryScope.createKotlinFile(filePath: String, @Language("kotlin") contents: String) =
    createFile(filePath, contents)


fun ProjectDirectoryScope.createKtsFile(filePath: String, @Language("kts") contents: String) =
    createFile(filePath, contents)
