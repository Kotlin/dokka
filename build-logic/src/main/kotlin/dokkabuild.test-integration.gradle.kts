/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("UnstableApiUsage")

import dokkabuild.utils.systemProperty
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

plugins {
    id("dokkabuild.base")
    `jvm-test-suite`
}

val integrationTest by tasks.registering {
    description = "Lifecycle task for running all integration tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
}

tasks.withType<Test>().configureEach {
    setForkEvery(1)
    maxHeapSize = "2G"
    dokkaBuild.integrationTestParallelism.orNull?.let { parallelism ->
        maxParallelForks = parallelism
    }

    systemProperty.inputProperty("dokkaVersion", provider { project.version.toString() })
    systemProperty.inputProperty("dokkaVersionOverride", dokkaBuild.integrationTestDokkaVersionOverride)
        .optional(true)

    val useK2 = dokkaBuild.integrationTestUseK2
    systemProperty.inputProperty("org.jetbrains.dokka.experimental.tryK2", useK2)
        .optional(true)

    useJUnitPlatform {
        if (useK2.get()) excludeTags("onlyDescriptors", "onlyDescriptorsMPP")
    }
    systemProperty.inputProperty("isExhaustive", dokkaBuild.integrationTestExhaustive)

    // allow inspecting projects in temporary dirs after a test fails
    systemProperty.inputProperty(
        "junit.jupiter.tempdir.cleanup.mode.default",
        dokkaBuild.isCI.map { if (it) "ALWAYS" else "ON_SUCCESS" }
    )

    inputs.property("ENABLE_DEBUG", providers.systemProperty("ENABLE_DEBUG"))
        .optional(true)

    testLogging {
        exceptionFormat = FULL
        events(SKIPPED, FAILED)
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

    // For validation, on CI the generated output is uploaded, so the test must produce output in
    // DOKKA_TEST_OUTPUT_PATH. For Gradle up-to-date checks the output dir must be specified.
    val testOutputPath = System.getenv("DOKKA_TEST_OUTPUT_PATH")
    inputs.property("testOutputPath", testOutputPath).optional(true)
    if (testOutputPath != null) {
        outputs.dir(testOutputPath).withPropertyName("testOutput")
    }

    // The tests produce report data and generated Dokka output.
    // Always cache them so Gradle can skip running integration tests if nothing has changed.
    outputs.cacheIf("always cache") { true }
}

testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            useJUnitJupiter()

            targets.configureEach {
                testTask.configure {
                    doFirst {
                        logger.info("running $path with javaLauncher:${javaLauncher.orNull?.metadata?.javaRuntimeVersion}")
                    }
                }
            }
        }
    }
}

integrationTest.configure {
    dependsOn(testing.suites)
}

tasks.check {
    dependsOn(integrationTest)
}
