package dev.adamko.dokkatoo.utils

import dev.adamko.dokkatoo.utils.GradleProjectTest.Companion.dokkaVersionOverride
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.readText
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


// utils for testing using Gradle TestKit


class GradleProjectTest(
    override val projectDir: Path,
) : ProjectDirectoryScope {

    constructor(
        testProjectName: String,
        baseDir: Path = funcTestTempDir,
    ) : this(projectDir = baseDir.resolve(testProjectName))

    /** Args that will be added to every [runner] */
    val defaultRunnerArgs: MutableList<String> = mutableListOf(
        // disable the logging task so the tests work consistently on local machines and CI/CD
        "-P" + "dev.adamko.dokkatoo.tasks.logHtmlPublicationLinkEnabled=false"
    )

    val runner: GradleRunner
        get() = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withJvmArguments(
                "-XX:MaxMetaspaceSize=512m",
                "-XX:+AlwaysPreTouch", // https://github.com/gradle/gradle/issues/3093#issuecomment-387259298
            )
            .addArguments(*defaultRunnerArgs.toTypedArray())

    companion object {

        val dokkaVersionOverride: String? by optionalSystemProperty()

        /** file-based Maven repositories with Dokka dependencies */
        val devMavenRepositories: List<Path> by systemProperty { repos ->
            repos.split(",").map { Path(it) }
        }

        val projectTestTempDir: Path by systemProperty(Paths::get)

        val exampleProjectDataPath: Path by systemProperty(Paths::get)

        /** Temporary directory for the functional tests */
        val funcTestTempDir: Path by lazy {
            projectTestTempDir.resolve("functional-tests")
        }

        /** Dokka Source directory that contains Gradle projects used for integration tests */
        val integrationTestProjectsDir: Path by systemProperty(Paths::get)

        /** Dokka Source directory that contains example Gradle projects */
        val exampleProjectsDir: Path by systemProperty(Paths::get)
    }
}


///**
// * Load a project from the [GradleProjectTest.dokkaSrcIntegrationTestProjectsDir]
// */
//fun gradleKtsProjectIntegrationTest(
//  testProjectName: String,
//  build: GradleProjectTest.() -> Unit,
//): GradleProjectTest =
//  GradleProjectTest(
//    baseDir = GradleProjectTest.dokkaSrcIntegrationTestProjectsDir,
//    testProjectName = testProjectName,
//  ).apply(build)


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
        gradleProperties += """
            enableDokkatoo=true
        """

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

        gradleProperties = """
      |kotlin.mpp.stability.nowarn=true
      |org.gradle.cache=true
      |
    """.trimMargin()

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

//    val testMavenRepoRelativePath: String
//        get() = projectDir
//            .relativize(GradleProjectTest.testMavenRepoDir)
//            .invariantSeparatorsPathString

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
        val dokkatooTestRepo = """
        |exclusiveContent {
        |  forRepositories(
        |${reposSpecs.prependIndent("   ")}
        |  )
        |  filter {
        |    includeGroup("org.jetbrains.dokka")
        |    includeGroup("dev.adamko.dokkatoo")
        |    includeGroup("dev.adamko.dokkatoo-html")
        |    includeGroup("dev.adamko.dokkatoo-javadoc")
        |    includeGroup("dev.adamko.dokkatoo-jekyll")
        |    includeGroup("dev.adamko.dokkatoo-gfm")
        |  }
        |}
    """.trimMargin()

        return """
        |pluginManagement {
        |  repositories {
        |${dokkatooTestRepo.prependIndent("    ")}
        |    mavenCentral()
        |    gradlePluginPortal()
        |  }
        |}
        |
        |@Suppress("UnstableApiUsage")
        |dependencyResolutionManagement {
        |  repositories {
        |${dokkatooTestRepo.prependIndent("    ")}
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


/** Set the content of `gradle.properties` */
@delegate:Language("properties")
var ProjectDirectoryScope.gradleProperties: String by TestProjectFileDelegate(
    /* language=text */ "gradle.properties"
)


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
