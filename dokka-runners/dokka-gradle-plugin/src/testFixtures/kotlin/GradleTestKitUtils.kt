/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import org.jetbrains.dokka.gradle.utils.GradleProjectTest.Companion.dokkaVersionOverride
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.readText
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


// utils for testing using Gradle TestKit


class GradleProjectTest(
    override val projectDir: Path,
) : ProjectDirectoryScope {

    val gradleProperties = GradleProperties()

    fun gradleProperties(config: GradleProperties.() -> Unit) {
        gradleProperties.config()
    }

    /**
     * The arguments in this class will be used to build the project's
     * `gradle.properties` file.
     */
    data class GradleProperties(
        val gradle: GradleArgs = GradleArgs(),
        val dokka: DokkaArgs = DokkaArgs(),
        val kotlin: KotlinArgs = KotlinArgs(),
    ) {
        fun dokka(config: DokkaArgs.() -> Unit): Unit = dokka.config()
        fun gradle(config: GradleArgs.() -> Unit): Unit = gradle.config()
        fun kotlin(config: KotlinArgs.() -> Unit): Unit = kotlin.config()

        /** Gradle specific options. */
        data class GradleArgs(
            var logLevel: LogLevel? = LogLevel.LIFECYCLE,
            var stacktrace: Boolean? = true,
            var debug: Boolean? = null,
            var buildCache: Boolean? = true,
            var buildCacheDebugLog: Boolean? = null,
            var configurationCache: Boolean? = null,
            var configureOnDemand: Boolean? = null,
            var continueOnFailure: Boolean? = null,
            var parallel: Boolean? = null,
            var warningMode: org.gradle.api.logging.configuration.WarningMode? = null,
            /** Will be enabled by default in Gradle 9.0 */
            var kotlinDslSkipMetadataVersionCheck: Boolean? = true,
            var daemonIdleTimeout: Duration? = 30.seconds,
            /**
             * Specifies the scheduling priority for the Gradle daemon and all processes launched by it.
             */
            var daemonSchedulingPriority: SchedulingPriority? = SchedulingPriority.Low,
            var maxWorkers: Int? = null,
            val jvmArgs: JvmArgs = JvmArgs(),

            // maybe also implement these flags? Although there's suitable tests for them at present.
            // org.gradle.projectcachedir=(directory)
            // org.gradle.unsafe.isolated-projects=(true,false)
            // org.gradle.vfs.verbose=(true,false)
            // org.gradle.vfs.watch=(true,false)
        ) {
            enum class SchedulingPriority { Low, Normal }

            fun jvm(config: JvmArgs.() -> Unit): Unit = jvmArgs.config()
        }

        /** Kotlin specific options. */
        data class KotlinArgs(
            var mppStabilityWarning: Boolean? = true,
        )

        /** Dokka specific options. */
        data class DokkaArgs(
            var enableV2Plugin: Boolean? = true,
            var disableV2PluginWarning: Boolean? = enableV2Plugin,
            var enableLogHtmlPublicationLink: Boolean? = false,
        )

        /** Gradle Daemon JVM args. */
        data class JvmArgs(
            @Suppress("PropertyName")
            var Xmx: String? = null,
            var fileEncoding: String? = "UTF-8",
            var maxMetaspaceSize: String? = "512m",
            /** Enable `AlwaysPreTouch` by default https://github.com/gradle/gradle/issues/3093#issuecomment-387259298 */
            var alwaysPreTouch: Boolean = true,
        ) {
            fun buildString(): String = buildList {
                fun addNotNull(key: String, value: String?) {
                    value?.let { add("$key$it") }
                }

                addNotNull("-Xmx", Xmx)
                addNotNull("-XX:MaxMetaspaceSize=", maxMetaspaceSize)
                addNotNull("-Dfile.encoding=", fileEncoding)
                if (alwaysPreTouch) add("-XX:+AlwaysPreTouch")
            }.joinToString(" ")
        }


        internal fun toGradleProperties(): Map<String, String> = buildMap {
            fun putNotNull(key: String, value: Any?) {
                value?.let { put(key, value.toString()) }
            }

            with(dokka) {
                putNotNull("org.jetbrains.dokka.experimental.gradlePlugin.enableV2", enableV2Plugin)
                putNotNull("org.jetbrains.dokka.experimental.gradlePlugin.enableV2.quiet", enableV2Plugin)
                putNotNull("org.jetbrains.dokka.gradle.enableLogHtmlPublicationLink", enableLogHtmlPublicationLink)
            }

            with(kotlin) {
                putNotNull("kotlin.mpp.stability.nowarn", mppStabilityWarning?.let { !it })
            }

            with(gradle) {
                putNotNull("org.gradle.caching", buildCache)
                putNotNull("org.gradle.caching.debug", buildCacheDebugLog)
                putNotNull("org.gradle.configuration-cache", configurationCache)
                putNotNull("org.gradle.configureondemand", configureOnDemand)
                putNotNull("org.gradle.continue", continueOnFailure)
                putNotNull("org.gradle.daemon.idletimeout", daemonIdleTimeout?.inWholeMilliseconds)
                putNotNull("org.gradle.priority", daemonSchedulingPriority)
                putNotNull("org.gradle.debug", debug)
                putNotNull("org.gradle.logging.level", logLevel?.name?.lowercase())
                putNotNull("org.gradle.workers.max", maxWorkers)
                putNotNull("org.gradle.parallel", parallel)
                putNotNull("org.gradle.stacktrace", stacktrace)
                putNotNull("org.gradle.warning.mode", warningMode)
                jvmArgs.buildString().takeIf { it.isNotBlank() }?.let {
                    put("org.gradle.jvmargs", it)
                }
            }
        }
    }

    constructor(
        testProjectName: String,
        baseDir: Path = funcTestTempDir,
    ) : this(projectDir = baseDir.resolve(testProjectName))

    val runner: GradleRunner
        get() = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .updateGradleProperties(gradleProperties)

    companion object {
        val dokkaVersionOverride: String? by optionalSystemProperty()

        /** file-based Maven repositories with Dokka dependencies */
        val devMavenRepositories: List<Path> by systemProperty { repos ->
            repos.split(",").map { Path(it) }
        }

        private val projectTestTempDir: Path by systemProperty(Paths::get)

        /** Temporary directory for the functional tests */
        val funcTestTempDir: Path by lazy {
            projectTestTempDir.resolve("functional-tests")
        }
    }
}


/**
 * Builder for testing a Gradle project that uses Kotlin script DSL and creates default
 * `settings.gradle.kts` and `gradle.properties` files.
 *
 * @param[testProjectName] the path of the project directory, relative to [baseDir
 */
fun gradleKtsProjectTest(
    testProjectName: String,
    baseDir: Path = GradleProjectTest.funcTestTempDir,
    build: GradleProjectTest.() -> Unit,
): GradleProjectTest {
    return gradleProjectTest(
        testProjectName = testProjectName,
        baseDir = baseDir,
    ) {

        settingsGradleKts = """
            |rootProject.name = "test"
            |
            |${settingsRepositories()}
            |
            """.trimMargin()

        build()
    }
}

/**
 * Builder for testing a Gradle project that uses Groovy script and creates default,
 * `settings.gradle`, and `gradle.properties` files.
 *
 * @param[testProjectName] the name of the test, which should be distinct across the project
 */
fun gradleGroovyProjectTest(
    testProjectName: String,
    baseDir: Path = GradleProjectTest.funcTestTempDir,
    build: GradleProjectTest.() -> Unit,
): GradleProjectTest {
    return gradleProjectTest(
        testProjectName = testProjectName,
        baseDir = baseDir,
    ) {
        settingsGradle = """
            |rootProject.name = "test"
            |
            |${settingsRepositories()}
            |
            """.trimMargin()

        build()
    }
}

private fun gradleProjectTest(
    testProjectName: String,
    baseDir: Path = GradleProjectTest.funcTestTempDir,
    build: GradleProjectTest.() -> Unit,
): GradleProjectTest {
    return GradleProjectTest(baseDir = baseDir, testProjectName = testProjectName).apply {
        build()
    }
}


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
        project.projectDir.resolve(filePath).readText()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        project.createFile(filePath, value)
    }
}

/** Delegate for reading and writing a [GradleProjectTest] file. */
class TestProjectFileDelegate(
    private val filePath: String,
) : ReadWriteProperty<ProjectDirectoryScope, String> {
    override fun getValue(thisRef: ProjectDirectoryScope, property: KProperty<*>): String =
        thisRef.projectDir.resolve(filePath).readText()

    override fun setValue(thisRef: ProjectDirectoryScope, property: KProperty<*>, value: String) {
        thisRef.createFile(filePath, value)
    }
}


@DslMarker
annotation class ProjectDirectoryDsl

@ProjectDirectoryDsl
interface ProjectDirectoryScope {
    val projectDir: Path

    @Language("kts")
    fun settingsRepositories(): String {
        val reposSpecs = if (dokkaVersionOverride != null) {
            println("Dokka version overridden with $dokkaVersionOverride")
            // if `DOKKA_VERSION_OVERRIDE` environment variable is provided,
            //  we allow running tests on a custom Dokka version from specific repositories
            """
            maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/test"),
            maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev"),
            mavenCentral(),
            mavenLocal()
            """.trimIndent()
        } else {
            // otherwise - use locally published versions via `devMavenPublish`
            GradleProjectTest.devMavenRepositories.withIndex().joinToString(",\n") { (i, repoPath) ->
                // Exclusive repository containing local Dokka artifacts.
                // Must be compatible with both Groovy and Kotlin DSL.
                /* language=kts */
                """
                |maven {
                |    setUrl("${repoPath.invariantSeparatorsPathString}")
                |    name = "DokkaDevMavenRepo${i}"
                |}
                """.trimMargin()
            }
        }


        // must be compatible with both Kotlin DSL and Groovy DSL

        @Language("kts")
        val dokkaTestRepo = """
        |exclusiveContent {
        |  forRepositories(
        |${reposSpecs.prependIndent("   ")}
        |  )
        |  filter {
        |    includeGroup("org.jetbrains.dokka")
        |  }
        |}
    """.trimMargin()

        return """
        |pluginManagement {
        |  repositories {
        |${dokkaTestRepo.prependIndent("    ")}
        |    mavenCentral()
        |    gradlePluginPortal()
        |  }
        |}
        |
        |@Suppress("UnstableApiUsage")
        |dependencyResolutionManagement {
        |  repositories {
        |${dokkaTestRepo.prependIndent("    ")}
        |    mavenCentral()
        |  }
        |}
        |
      """.trimMargin()
    }
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
var ProjectDirectoryScope.settingsGradleKts: String by TestProjectFileDelegate("settings.gradle.kts")


/** Set the content of `build.gradle.kts` */
@delegate:Language("kts")
var ProjectDirectoryScope.buildGradleKts: String by TestProjectFileDelegate("build.gradle.kts")


/** Set the content of `settings.gradle` */
@delegate:Language("groovy")
var ProjectDirectoryScope.settingsGradle: String by TestProjectFileDelegate("settings.gradle")


/** Set the content of `build.gradle` */
@delegate:Language("groovy")
var ProjectDirectoryScope.buildGradle: String by TestProjectFileDelegate("build.gradle")

fun ProjectDirectoryScope.createKotlinFile(
    filePath: String,
    @Language("kotlin") contents: String
): File =
    createFile(filePath, contents)


fun ProjectDirectoryScope.createKtsFile(
    filePath: String,
    @Language("kts") contents: String
): File =
    createFile(filePath, contents)
