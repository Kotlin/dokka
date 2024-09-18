/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("UnstableApiUsage")

import dokkabuild.utils.systemProperty
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Disabled
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

plugins {
    id("dokkabuild.base")
    `jvm-test-suite`
}

val integrationTest by tasks.registering {
    description = "Lifecycle task for running all integration tests."
    group = VERIFICATION_GROUP
}

tasks.withType<Test>().configureEach {
    onlyIf("property `org.jetbrains.dokka.integration_test.skip` != true") {
        !dokkaBuild.integrationTestSkip.get()
    }

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

            dependencies {
                // test suites are independent by default (unlike the test source set), and must manually depend on the project
                implementation(project())
            }

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

//region project tests management

// set up task ordering - template projects (which are generally faster) should be tested before external projects
val jvmTestTask = tasks.withType<Test>().matching { it.name == "test" }
val testTemplateProjectsTasks = tasks.withType<Test>().matching { it.name.startsWith("testTemplateProject") }
val testExternalProjectsTasks = tasks.withType<Test>().matching { it.name.startsWith("testExternalProject") }

testTemplateProjectsTasks.configureEach {
    shouldRunAfter(jvmTestTask)
}
testExternalProjectsTasks.configureEach {
    shouldRunAfter(jvmTestTask)
    shouldRunAfter(testTemplateProjectsTasks)
}

// define lifecycle tasks for project tests
val testAllTemplateProjects by tasks.registering {
    description = "Lifecycle task for running all template-project tests"
    group = VERIFICATION_GROUP
    dependsOn(testTemplateProjectsTasks)
    doNotTrackState("lifecycle task, should always run")
}

val testAllExternalProjects by tasks.registering {
    description = "Lifecycle task for running all external-project tests"
    group = VERIFICATION_GROUP
    shouldRunAfter(testAllTemplateProjects)
    dependsOn(testExternalProjectsTasks)
    doNotTrackState("lifecycle task, should always run")
}
//endregion

pluginManager.withPlugin("dokkabuild.kotlin-jvm") {
    extensions.configure<KotlinProjectExtension> {
        // integration test projects only contains test utils, and aren't published, so it doesn't matter about explicit API
        explicitApi = Disabled
    }
}
