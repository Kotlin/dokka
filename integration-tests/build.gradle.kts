import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

subprojects {
    sourceSets {
        create("integrationTest") {
            compileClasspath += sourceSets.main.get().output
            runtimeClasspath += sourceSets.main.get().output
        }
    }

    configurations.getByName("integrationTestImplementation") {
        extendsFrom(configurations.implementation.get())
    }

    configurations.getByName("integrationTestRuntimeOnly") {
        extendsFrom(configurations.runtimeOnly.get())
    }

    dependencies {
        implementation(project(":integration-tests"))
    }

    /**
     * Dokka's integration test task is not cacheable because the HTML outputs
     * it produces when running the tests are used for showcasing resulting documentation,
     * which does not work well with caching.
     *
     * At the moment there are two problems that do not allow to make it cacheable:
     *
     * 1. The task's inputs are such that changes in Dokka's code do not invalidate the cache,
     *    because it is run with the same version of Dokka ("DOKKA_VERSION") on the same
     *    test project inputs.
     * 2. The tests generate HTML output which is then used to showcase documentation.
     *    The outputs are usually copied to a location from which it will be served.
     *    However, if the test is cacheable, it produces no outputs, so no documentation
     *    to showcase. It needs to be broken into two separate tasks: one cacheable for running
     *    the tests and producing HTML output, and another non-cacheable for copying the output.
     *
     * @see [org.jetbrains.dokka.it.TestOutputCopier] for more details on showcasing documentation
     */
    @DisableCachingByDefault(because = "Contains incorrect inputs/outputs configuration, see the KDoc for details")
    abstract class NonCacheableIntegrationTest : Test()

    val integrationTest by tasks.register<NonCacheableIntegrationTest>("integrationTest") {
        maxHeapSize = "2G"
        description = "Runs integration tests."
        group = "verification"
        useJUnit()


        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath

        setForkEvery(1)
        project.properties["dokka_integration_test_parallelism"]?.toString()?.toIntOrNull()?.let { parallelism ->
            maxParallelForks = parallelism
        }
        environment(
            "isExhaustive",
            project.properties["dokka_integration_test_is_exhaustive"]?.toString()?.toBoolean()
                ?: System.getenv("DOKKA_INTEGRATION_TEST_IS_EXHAUSTIVE")?.toBoolean()
                ?: false.toString()
        )

        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events(TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }

    tasks.check {
        dependsOn(integrationTest)
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    api(project(":test-utils"))
    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    val jsoup_version: String by project
    implementation("org.jsoup:jsoup:$jsoup_version")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.12.0.202106070339-r")
}
