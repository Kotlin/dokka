/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("UnstableApiUsage")

import dokkabuild.utils.systemProperty
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.test-integration")
    id("dokkabuild.dev-maven-publish")
    `java-test-fixtures`
    alias(libs.plugins.kotlinxSerialization)
}

description = "Test the Dokka Gradle example projects."

dependencies {
    api(projects.utilities)

    testFixturesApi(libs.kotlin.test)
    testFixturesApi(libs.junit.jupiterApi)
    testFixturesApi(libs.junit.jupiterParams)

    testFixturesImplementation(platform(libs.kotlinxSerialization.bom))
    testFixturesImplementation(libs.kotlinxSerialization.json)

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

    devPublication("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")

    testFixturesApi(gradleTestKit())

    testFixturesApi(testFixtures("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion"))
    testFixturesApi(platform(libs.kotest.bom))
    testFixturesApi(libs.kotest.assertionsCore)
    testFixturesApi(libs.kotest.assertionsJson)
    testFixturesApi(libs.kotest.datatest)
    testFixturesApi(libs.kotest.property)
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.io.path.ExperimentalPathApi")
    }
}

tasks.withType<Test>().configureEach {
    val enableDebug = providers.environmentVariable("ENABLE_DEBUG")
    inputs.property("enableDebug", enableDebug).optional(true)
    environment("ENABLE_DEBUG", enableDebug.getOrElse("false"))

    systemProperty("hostGradleUserHome", gradle.gradleUserHomeDir.invariantSeparatorsPath)
}

val gradleExamplesDir: Path = projectDir.resolve("../../examples/gradle").toPath()


gradleExamplesDir
    .listDirectoryEntries()
    .filter { it.isDirectory() }
    .forEach { exampleProjectDir ->

        val prettyName = exampleProjectDir.name
            .substringBefore("-example")
            .split(Regex("\\W+"))
            .joinToString("") { it.uppercaseFirstChar() }

        registerExampleTest(
            name = "test$prettyName",
            projectPath = exampleProjectDir.name,
        )
    }


/**
 * Create a new [JvmTestSuite] for a Gradle project.
 *
 * @param[projectPath] path to the Gradle project that will be tested by this suite, relative to [gradleExamplesDir].
 * The directory will be passed as a system property, `templateProjectDir`.
 */
fun registerExampleTest(
    name: String,
    projectPath: String,
    configure: JvmTestSuite.() -> Unit = {},
) {
    val exampleProjectDir = gradleExamplesDir.resolve(projectPath)

    testing.suites.register<JvmTestSuite>(name) {
        targets.configureEach {
            testTask.configure {
                // Pass the template dir in as a property, so it is accessible in tests.
                systemProperty
                    .inputDirectory("exampleProjectDir", exampleProjectDir.toFile())
                    .withPathSensitivity(RELATIVE)

                systemProperty
                    .inputDirectory("expectedDataDir", layout.projectDirectory.dir("src/$name/expectedData"))
                    .withPathSensitivity(RELATIVE)

                systemProperty
                    .outputDirectory("projectTestTempDir", layout.buildDirectory.dir("test-temp-dir/$name"))

                devMavenPublish.configureTask(this)
            }
        }
        configure()
    }
}


sourceSets.configureEach {
    kotlin.setSrcDirs(listOf("src/$name/kotlin"))
    resources.setSrcDirs(listOf("src/$name/resources"))
    java.setSrcDirs(emptyList<File>())
}

testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            useJUnitJupiter()

            dependencies {
                // test suites are independent by default (unlike the test source set), and must manually depend on the project
                implementation(project())
                implementation(testFixtures(project()))
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
