/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle


import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.dokka.gradle.utils.GradlePropertiesBuilder
import org.jetbrains.dokka.gradle.utils.ProjectDirectoryScope
import org.jetbrains.dokka.it.gradle.AbstractGradleIntegrationTest.Companion.updateProjectLocalMavenDir
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import org.junit.platform.commons.logging.Logger
import org.junit.platform.commons.logging.LoggerFactory
import org.junit.platform.commons.support.ReflectionSupport
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.*
import kotlin.reflect.KClass
import kotlin.streams.asStream

/**
 * A Dokka Gradle Plugin test.
 *
 * Annotate a test function with `@DokkaGradlePluginTest` to run the test multiple times with all
 * configured Android/Gradle/Kotlin/Dokka versions.
 *
 * Add a [GradleProject] parameter.
 *
 * [GradleTestExtension] will generate a new project in a temporary directory for each test.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@TestTemplate
@ExtendWith(GradleTestExtension::class)
annotation class DokkaGradlePluginTest(
    val sourceProjectDir: String,
    val projectInitializer: KClass<out GradleTestProjectInitializer> = GradleTestProjectInitializer.Default::class,
    val gradlePropertiesProvider: KClass<out GradlePropertiesProvider> = GradlePropertiesProvider.Default::class,
)


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("AndroidGradlePlugin")
annotation class TestAndroidGradlePlugin


class GradleTestExtension :
    TestTemplateInvocationContextProvider,
    BeforeAllCallback,
    BeforeEachCallback {

    override fun beforeAll(context: ExtensionContext) {
        installFailureTracker(context)
    }

    override fun beforeEach(context: ExtensionContext) {
        installFailureTracker(context)
    }

    override fun supportsTestTemplate(context: ExtensionContext): Boolean = true

    override fun provideTestTemplateInvocationContexts(
        context: ExtensionContext
    ): Stream<TestTemplateInvocationContext> {
        val isAndroidTest = context.requiredTestMethod.isAnnotationPresent(TestAndroidGradlePlugin::class.java)

        val baseDgpTestDir = Files.createTempDirectory("dgp-test")

        val dgpTest = context.requiredTestMethod.getAnnotation(DokkaGradlePluginTest::class.java)
        val projectInitializer = ReflectionSupport.newInstance(dgpTest.projectInitializer.java)
        val sourceProjectDir = dgpTest.sourceProjectDir

        val gradlePropertiesProvider = ReflectionSupport.newInstance(dgpTest.gradlePropertiesProvider.java)
        val gradleProperties = gradlePropertiesProvider.get()

        return allTestedVersions
            .filter { if (isAndroidTest) it.agp != null else true }
            .map { testedVersions ->

                val projectTmpDir = baseDgpTestDir.resolve(testedVersions.dashSeparated)
                context.getStore(NAMESPACE).put(
                    "project-tmp-dir-${projectTmpDir.invariantSeparatorsPathString}",
                    CloseablePath(projectTmpDir, context),
                )

                projectInitializer.initialize(
                    source = Path(sourceProjectDir),
                    destination = projectTmpDir,
                    testedVersions = testedVersions,
                    gradleProperties = gradleProperties,
                )

                GradleProjectTestTemplate(
                    projectDir = projectTmpDir,
                    testedVersions = testedVersions,
                )
            }
            .asStream()
    }

    private class GradleProjectTestTemplate(
        private val projectDir: Path,
        private val testedVersions: TestVersionCombination,
    ) : TestTemplateInvocationContext {
        override fun getDisplayName(invocationIndex: Int): String =
            "[$invocationIndex] ${testedVersions.displayName}"

        override fun getAdditionalExtensions(): List<Extension> =
            listOf(
                GradleProjectParameterResolver(projectDir, testedVersions),
            )
    }

    private class GradleProjectParameterResolver(
        private val projectDir: Path,
        private val testedVersions: TestVersionCombination,
    ) : ParameterResolver {
        override fun supportsParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext
        ): Boolean = parameterContext.parameter.type == GradleProject::class.java

        override fun resolveParameter(
            parameterContext: ParameterContext?,
            extensionContext: ExtensionContext?
        ): GradleProject {
            val gradleRunner = setupGradleRunner(projectDir, testedVersions.gradle)
            return GradleProject(projectDir, gradleRunner, testedVersions)
        }
    }

    /**
     * JUnit will automatically delete [path] on failures,
     * depending on the configured [tmpDirCleanupMode].
     */
    private class CloseablePath(
        private val path: Path,
        private val context: ExtensionContext,
    ) : CloseableResource {
        override fun close() {
            if (
                tmpDirCleanupMode == CleanupMode.NEVER
                || (tmpDirCleanupMode == CleanupMode.ON_SUCCESS && context.selfOrChildFailed())
            ) {
                logger.info { "Skipping cleanup of temp dir $path due to cleanup mode: $tmpDirCleanupMode" }
                return
            }
            path.deleteRecursively()
        }

        companion object {
            private val logger: Logger = LoggerFactory.getLogger(CloseablePath::class.java)
        }
    }

    companion object {
        private val NAMESPACE: Namespace = Namespace.create(GradleTestExtension::class.java)

        //region test failure tracker
        /**
         * Track if a test, or any of its children, has failed.
         *
         * Used to determine if the temporary project directory should be deleted after successes.
         * (Don't delete failures when running on a developer machine to allow for further investigation.)
         *
         * Inspired by [TempDirectory](https://github.com/junit-team/junit5/blob/r5.11.3/junit-jupiter-engine/src/main/java/org/junit/jupiter/engine/extension/TempDirectory.java).
         */
        private fun installFailureTracker(context: ExtensionContext) {
            context.getStore(NAMESPACE).put(FAILURE_TRACKER, CloseableResource {
                context.parent.ifPresent { it: ExtensionContext ->
                    if (context.selfOrChildFailed()) {
                        it.getStore(NAMESPACE).put(CHILD_FAILED, true)
                    }
                }
            })
        }

        /** @see [installFailureTracker] */
        private const val FAILURE_TRACKER: String = "failure.tracker"

        /** @see [installFailureTracker] */
        private const val CHILD_FAILED: String = "child.failed"

        /** @see [installFailureTracker] */
        private fun ExtensionContext.selfOrChildFailed(): Boolean {
            return executionException.isPresent
                    || getStore(NAMESPACE).getOrDefault(CHILD_FAILED, Boolean::class.java, false)
        }
        //endregion

        /** Kotlin Gradle Plugin versions to test. */
        private val testedKgpVersions = listOf("1.9.25", "2.0.21")

        /** Android Gradle Plugin versions to test. */
        private val testedAgpVersions = listOf("7.4.2", "8.7.1")

        /** Gradle versions to test. */
        private val testedGradleVersions = listOf("7.6.4", "8.10.2")

        /** Dokka Gradle Plugin versions to test. */
        private val testedDgpVersions = listOf("2.0.20-SNAPSHOT")

        private val allTestedVersions: Sequence<TestVersionCombination> = sequence {
            testedGradleVersions.forEach { gradle ->
                testedKgpVersions.forEach { kgp ->
                    testedAgpVersions.forEach { agp ->
                        testedDgpVersions.forEach { dgp ->
                            yield(
                                TestVersionCombination(
                                    kgp = kgp,
                                    gradle = gradle,
                                    dgp = dgp,
                                    agp = agp,
                                )
                            )
                        }
                    }
                }
            }
        }
            .filter { it.isValid() }
            .sorted()

        private val tmpDirCleanupMode: CleanupMode =
            System.getProperty(TempDir.DEFAULT_CLEANUP_MODE_PROPERTY_NAME)
                ?.let { CleanupMode.valueOf(it) }
                ?: CleanupMode.DEFAULT

        private fun setupGradleRunner(projectDir: Path, gradle: SemVer): GradleRunner {
            return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withJetBrainsCachedGradleVersion(gradle.version)
                .withReadOnlyDependencyCache()
                .forwardOutput()
        }
    }
}


/**
 * Initialise a Gradle Test project into a directory.
 *
 * The source directory must not be modified.
 */
fun interface GradleTestProjectInitializer {
    fun initialize(
        source: Path,
        destination: Path,
        testedVersions: TestVersionCombination,
        gradleProperties: Map<String, String>
    )

    object Default : GradleTestProjectInitializer {
        override fun initialize(
            source: Path,
            destination: Path,
            testedVersions: TestVersionCombination,
            gradleProperties: Map<String, String>,
        ) {
            source.copyToRecursively(destination, overwrite = true, followLinks = false)

            destination.updateProjectLocalMavenDir()

            val gradlePropertiesFile = destination.resolve("gradle.properties").apply {
                deleteIfExists()
                createFile()
            }

            gradlePropertiesFile.writeText(
                gradleProperties.entries
                    .map { (k, v) -> "$k=$v" }
                    .sorted()
                    .joinToString(separator = "\n", postfix = "\n")
            )

            fun SemVer.pluginVersion(): String = "version \"$version\""

            destination.walk().filter { it.isRegularFile() }.forEach { file ->
                file.writeText(
                    file.readText()
                        .replace("/* %{KGP_VERSION} */", testedVersions.kgp.pluginVersion())
                        .replace("/* %{DGP_VERSION} */", testedVersions.dgp.pluginVersion())
                        .run {
                            if (testedVersions.agp != null) {
                                replace("/* %{AGP_VERSION} */", testedVersions.agp.pluginVersion())
                            } else {
                                this
                            }
                        }
                )
            }

            if (testedVersions.agp != null) {
                destination.resolve("local.properties").apply {
                    // TODO remove hardcoded path
                    writeText(
                        """
                        |sdk.dir=/opt/homebrew/share/android-commandlinetools
                        |
                        """.trimMargin()
                    )
                }

                gradlePropertiesFile.appendText(
                    """
                    |android.useAndroidX=true
                    |
                    """.trimMargin()
                )
            }
        }
    }
}

fun interface GradlePropertiesProvider {
    fun get(): Map<String, String>

    object Default : GradlePropertiesProvider {
        override fun get(): Map<String, String> {
            return GradlePropertiesBuilder().build()
        }
    }
}

class GradleProject(
    override val projectDir: Path,
    val runner: GradleRunner,
    val versions: TestVersionCombination,
) : ProjectDirectoryScope


data class TestVersionCombination(
    /** Kotlin Gradle Plugin version. */
    val kgp: SemVer,
    /** Gradle version. */
    val gradle: SemVer,
    /** Dokka Gradle Plugin version. */
    val dgp: SemVer,
    /** Android Gradle Plugin version. */
    val agp: SemVer?,
) : Comparable<TestVersionCombination> {

    constructor(
        kgp: String,
        gradle: String,
        dgp: String,
        agp: String?,
    ) : this(
        kgp = SemVer(kgp),
        gradle = SemVer(gradle),
        dgp = SemVer(dgp),
        agp = agp?.let { SemVer(it) },
    )

    /**
     * Join the versions in a dash-separated-string, for use in file names.
     */
    fun isValid(): Boolean {
        if (agp != null) {
            if (agp.major != gradle.major) {
                return false
            }
        }
        return true
    }

    /**
     * Join the versions in a dash-separated-string, for use in file names.
     */
    val dashSeparated: String =
        buildList {
            add("gradle-$gradle")
            add("kgp-$kgp")
            if (agp != null) add("agp-$agp")
        }.joinToString("-")

    /**
     * Pretty display name, for use in JUnit test names.
     */
    val displayName: String =
        buildList {
            add("Gradle: $gradle")
            add("KGP: $kgp")
            if (agp != null) add("AGP: $agp")
        }.joinToString()

    override fun compareTo(other: TestVersionCombination): Int {
        return this.toString().compareTo(other.toString())
    }
}


data class SemVer(
    val version: String,
) {
    val major: Int
    val minor: Int
    val patch: Int
    val prerelease: String?
    val metadata: String?

    init {
        val match = semverRegex.matchEntire(version) ?: error("Invalid version '$version'")
        major = match.groups["major"]?.value?.toIntOrNull() ?: error("missing major version in '$version'")
        minor = match.groups["minor"]?.value?.toIntOrNull() ?: error("missing minor version in '$version'")
        patch = match.groups["patch"]?.value?.toIntOrNull() ?: error("missing patch version in '$version'")
        prerelease = match.groups["prerelease"]?.value
        metadata = match.groups["buildMetadata"]?.value
    }

    override fun toString(): String = version

    companion object {
        // https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string
        private val semverRegex = Regex(
            """
            ^(?<major>0|[1-9]\d*)\.(?<minor>0|[1-9]\d*)\.(?<patch>0|[1-9]\d*)(?:-(?<prerelease>(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+(?<buildMetadata>[0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?${'$'}
            """.trimIndent()
        )
    }
}
