/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("UnstableApiUsage")

import dokkabuild.tasks.GitCheckoutTask
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Disabled

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.dev-maven-publish")
    `jvm-test-suite`
    `java-test-fixtures`
}

dependencies {
    api(projects.utilities)

    api(libs.jsoup)

    api(libs.kotlin.test)
    api(libs.junit.jupiterApi)
    api(libs.junit.jupiterParams)

    api(gradleTestKit())

    val dokkaVersion = project.version.toString()
    // We're using Gradle included-builds and dependency substitution, so we
    // need to use the Gradle project name, *not* the published Maven artifact-id
    devPublication("org.jetbrains.dokka:plugin-all-modules-page:$dokkaVersion")
    devPublication("org.jetbrains.dokka:analysis-kotlin-api:$dokkaVersion")
    devPublication("org.jetbrains.dokka:analysis-kotlin-descriptors:$dokkaVersion")
    devPublication("org.jetbrains.dokka:analysis-kotlin-symbols:$dokkaVersion")
    devPublication("org.jetbrains.dokka:analysis-markdown-jb:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-android-documentation:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-base:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-base-test-utils:$dokkaVersion")
    devPublication("org.jetbrains.dokka:dokka-core:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-gfm:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-gfm-template-processing:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-javadoc:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-jekyll:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-jekyll-template-processing:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-kotlin-as-java:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-mathjax:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-templating:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-versioning:$dokkaVersion")

    devPublication("org.jetbrains.dokka:runner-gradle-plugin-classic:$dokkaVersion")
}

kotlin {
    // this project only contains test utils and isn't published, so it doesn't matter about explicit API
    explicitApi = Disabled

    compilerOptions {
        optIn.add("kotlin.io.path.ExperimentalPathApi")
    }
}

val aggregatingProject = gradle.includedBuild("dokka")
val templateSettingsGradleKts = layout.projectDirectory.file("projects/template.settings.gradle.kts")
val templateProjectsDir = layout.projectDirectory.dir("projects")

tasks.withType<Test>().configureEach {
    setForkEvery(1)
    maxHeapSize = "2G"
    dokkaBuild.integrationTestParallelism.orNull?.let { parallelism ->
        maxParallelForks = parallelism
    }

    val useK2 = dokkaBuild.integrationTestUseK2.get()

    useJUnitPlatform {
        if (useK2) excludeTags("onlyDescriptors", "onlyDescriptorsMPP")
    }

    systemProperty("org.jetbrains.dokka.experimental.tryK2", useK2)
    // allow inspecting projects in temporary dirs after a test fails
    systemProperty(
        "junit.jupiter.tempdir.cleanup.mode.default",
        dokkaBuild.isCI.map { isCi -> if (isCi) "ALWAYS" else "ON_SUCCESS" }.get(),
    )

    val dokkaVersion = provider { project.version.toString() }
    inputs.property("dokkaVersion", dokkaVersion)
    doFirst("set DOKKA_VERSION environment variable (workaround for https://github.com/gradle/gradle/issues/24267)") {
        environment("DOKKA_VERSION", dokkaVersion.get())
    }

    // environment() isn't Provider API compatible yet https://github.com/gradle/gradle/issues/11534
    dokkaBuild.integrationTestExhaustive.orNull?.let { exhaustive ->
        environment("isExhaustive", exhaustive)
    }
    dokkaBuild.androidSdkDir.orNull?.let { androidSdkDir ->
        environment("ANDROID_HOME", androidSdkDir.invariantSeparatorsPath)
    }

    testLogging {
        exceptionFormat = FULL
        events(SKIPPED, FAILED)
        showExceptions = true
        showCauses = true
        showStackTraces = true
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

        // register a separate test suite for each 'template' project
        registerTestProjectSuite(
            "testTemplateProjectAndroid",
            "it-android-0",
            jvm = JavaLanguageVersion.of(17), // AGP requires JVM 17+
        )
        registerTestProjectSuite("testTemplateProjectBasic", "it-basic")
        registerTestProjectSuite("testTemplateProjectBasicGroovy", "it-basic-groovy")
        registerTestProjectSuite("testTemplateProjectCollector", "it-collector-0")
        registerTestProjectSuite("testTemplateProjectConfiguration", "it-configuration")
        registerTestProjectSuite("testTemplateProjectJsIr", "it-js-ir-0")
        registerTestProjectSuite("testTemplateProjectMultimodule0", "it-multimodule-0")
        registerTestProjectSuite("testTemplateProjectMultimodule1", "it-multimodule-1")
        registerTestProjectSuite("testTemplateProjectMultimoduleVersioning", "it-multimodule-versioning-0")
        registerTestProjectSuite("testTemplateProjectMultimoduleInterModuleLinks", "it-multimodule-inter-module-links")
        registerTestProjectSuite("testTemplateProjectMultiplatform", "it-multiplatform-0")
        registerTestProjectSuite("testTemplateProjectTasksExecutionStress", "it-sequential-tasks-execution-stress")
        registerTestProjectSuite("testTemplateProjectWasmBasic", "it-wasm-basic")
        registerTestProjectSuite("testTemplateProjectWasmJsWasiBasic", "it-wasm-js-wasi-basic")

        registerTestProjectSuite(
            "testExternalProjectKotlinxCoroutines",
            "coroutines/kotlinx-coroutines",
            jvm = JavaLanguageVersion.of(11) // kotlinx.coroutines requires JVM 11+ https://github.com/Kotlin/kotlinx.coroutines/issues/3665
        ) {
            targets.configureEach {
                testTask.configure {
                    dependsOn(checkoutKotlinxCoroutines)
                    // register the whole directory as an input because it contains the git diff
                    inputs
                        .dir(templateProjectsDir.file("coroutines"))
                        .withPropertyName("coroutinesProjectDir")
                }
            }
        }
        registerTestProjectSuite(
            "testExternalProjectKotlinxSerialization",
            "serialization/kotlinx-serialization",
            jvm = JavaLanguageVersion.of(11) // https://github.com/Kotlin/kotlinx.serialization/blob/1116f5f13a957feecda47d5e08b0aa335fc010fa/gradle/configure-source-sets.gradle#L9
        ) {
            targets.configureEach {
                testTask.configure {
                    dependsOn(checkoutKotlinxSerialization)
                    // register the whole directory as an input because it contains the git diff
                    inputs
                        .dir(templateProjectsDir.file("serialization"))
                        .withPropertyName("serializationProjectDir")
                }
            }
        }
        registerTestProjectSuite("testUiShowcaseProject", "ui-showcase")
    }
}


/**
 * Create a new [JvmTestSuite] for a Gradle project.
 *
 * @param[projectPath] path to the Gradle project that will be tested by this suite, relative to [templateProjectsDir].
 * The directory will be passed as a system property, `templateProjectDir`.
 */
fun TestingExtension.registerTestProjectSuite(
    name: String,
    projectPath: String,
    jvm: JavaLanguageVersion? = null,
    configure: JvmTestSuite.() -> Unit = {},
) {
    val templateProjectDir = templateProjectsDir.dir(projectPath)

    suites.register<JvmTestSuite>(name) {
        targets.configureEach {
            testTask.configure {
                // Register the project dir as a specific input, so changes in other projects don't affect the caching of this test
                inputs.dir(templateProjectDir)
                    .withPropertyName("templateProjectDir")
                    .withPathSensitivity(RELATIVE)

                // Pass the template dir in as a property, it is accessible in tests.
                systemProperty("templateProjectDir", templateProjectDir.asFile.invariantSeparatorsPath)

                inputs.file(templateSettingsGradleKts)
                    .withPropertyName("templateSettingsGradleKts")
                    .withPathSensitivity(RELATIVE)
                systemProperty(
                    "templateSettingsGradleKts",
                    templateSettingsGradleKts.asFile.invariantSeparatorsPath,
                )

                devMavenPublish.configureTask(this)

                if (jvm != null) {
                    javaLauncher = javaToolchains.launcherFor { languageVersion = jvm }
                }

                // For validation, on CI the generated output is uploaded, so the test must produce output in
                // DOKKA_TEST_OUTPUT_PATH. For Gradle up-to-date checks the output dir must be specified.
                val testOutputPath = System.getenv("DOKKA_TEST_OUTPUT_PATH")
                inputs.property("testOutputPath", testOutputPath).optional(true)
                if (testOutputPath != null) {
                    outputs.dir(testOutputPath).withPropertyName("testOutput")
                }
            }
        }
        configure()
    }
}

//region project tests management

// set up task ordering - template projects (which are generally faster) should be tested before external projects
val testTemplateProjectsTasks = tasks.withType<Test>().matching { it.name.startsWith("testTemplateProject") }
val testExternalProjectsTasks = tasks.withType<Test>().matching { it.name.startsWith("testExternalProject") }

testTemplateProjectsTasks.configureEach {
    shouldRunAfter(tasks.test)
}
testExternalProjectsTasks.configureEach {
    shouldRunAfter(tasks.test)
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

val integrationTest by tasks.registering {
    description = "Lifecycle task for running integration tests"
    // TODO - Refactor Maven and CLI integration tests to use Test Suites
    //      - Reimplement dokkabuild.test-integration.gradle.kts so that `integrationTest` is defined once there
    dependsOn(tasks.withType<Test>()) // all tests in this project are integration tests
}
//endregion

val checkoutKotlinxCoroutines by tasks.registering(GitCheckoutTask::class) {
    uri = "https://github.com/Kotlin/kotlinx.coroutines.git"
    commitId = "b78bbf518bd8e90e9ed2133ebdacc36441210cd6"
    destination = templateProjectsDir.dir("coroutines/kotlinx-coroutines")
}

val checkoutKotlinxSerialization by tasks.registering(GitCheckoutTask::class) {
    uri = "https://github.com/Kotlin/kotlinx.serialization.git"
    commitId = "ed1b05707ec27f8864c8b42235b299bdb5e0015c"
    destination = templateProjectsDir.dir("serialization/kotlinx-serialization")
}
