/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UnstableApiUsage") // jvm test suites

import dokkabuild.tasks.GenerateDokkaGradlePluginConstants
import dokkabuild.utils.skipTestFixturesPublications

plugins {
    id("dokkabuild.gradle-plugin")

    kotlin("plugin.serialization") version embeddedKotlinVersion

    id("dokkabuild.dev-maven-publish")
    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
    `java-test-fixtures`
    `jvm-test-suite`

    id("dokkabuild.publish-base")
    alias(libs.plugins.gradlePluginPublish)
}

description = "Gradle plugin for using Dokka Engine"

kotlin {
    compilerOptions {
        optIn.addAll(
            "kotlin.RequiresOptIn",
            "org.jetbrains.dokka.gradle.internal.DokkaInternalApi",
            "kotlin.io.path.ExperimentalPathApi",
        )
    }
    sourceSets {
        //region Keep the classic plugin in separate directories.
        // This helps with organisation and with its eventual removal.
        main {
            kotlin.srcDir("src/classicMain/kotlin")
        }
        test {
            kotlin.srcDir("src/classicTest/kotlin")
        }
        //endregion
    }
}

dependencies {
    // ideally there should be a 'dokka-core-api' dependency (that is very thin and doesn't drag in loads of unnecessary code)
    // that would be used as an implementation dependency, while dokka-core would be used as a compileOnly dependency
    // https://github.com/Kotlin/dokka/issues/2933
    implementation("org.jetbrains.dokka:dokka-core:${project.version}")

    compileOnly(libs.gradlePlugin.kotlin)
    compileOnly(libs.gradlePlugin.kotlin.klibCommonizerApi)
    compileOnly(libs.gradlePlugin.android)
    compileOnly(libs.gradlePlugin.androidApi)

    implementation(platform(libs.kotlinxSerialization.bom))
    implementation(libs.kotlinxSerialization.json)

    testFixturesImplementation(gradleApi())
    testFixturesImplementation(gradleTestKit())

    testFixturesCompileOnly("org.jetbrains.dokka:dokka-core:${project.version}")
    testFixturesImplementation(platform(libs.kotlinxSerialization.bom))
    testFixturesImplementation(libs.kotlinxSerialization.json)

    testFixturesApi(platform(libs.kotest.bom))
    testFixturesApi(libs.kotest.junit5Runner)
    testFixturesApi(libs.kotest.assertionsCore)
    testFixturesApi(libs.kotest.assertionsJson)
    testFixturesApi(libs.kotest.datatest)

    // don't define test dependencies here, instead define them in the testing.suites {} configuration below

    // We're using Gradle included-builds and dependency substitution, so we
    // need to use the Gradle project name, *not* the published Maven artifact-id
    devPublication("org.jetbrains.dokka:plugin-all-modules-page:${project.version}")
    devPublication("org.jetbrains.dokka:analysis-kotlin-api:${project.version}")
    devPublication("org.jetbrains.dokka:analysis-kotlin-descriptors:${project.version}")
    devPublication("org.jetbrains.dokka:analysis-kotlin-symbols:${project.version}")
    devPublication("org.jetbrains.dokka:analysis-markdown-jb:${project.version}")
    devPublication("org.jetbrains.dokka:plugin-android-documentation:${project.version}")
    devPublication("org.jetbrains.dokka:plugin-base:${project.version}")
    devPublication("org.jetbrains.dokka:plugin-base-test-utils:${project.version}")
    devPublication("org.jetbrains.dokka:dokka-core:${project.version}")
    devPublication("org.jetbrains.dokka:plugin-gfm:${project.version}")
    devPublication("org.jetbrains.dokka:plugin-gfm-template-processing:${project.version}")
    devPublication("org.jetbrains.dokka:plugin-javadoc:${project.version}")
    devPublication("org.jetbrains.dokka:plugin-jekyll:${project.version}")
    devPublication("org.jetbrains.dokka:plugin-jekyll-template-processing:${project.version}")
    devPublication("org.jetbrains.dokka:plugin-kotlin-as-java:${project.version}")
    devPublication("org.jetbrains.dokka:plugin-mathjax:${project.version}")
    devPublication("org.jetbrains.dokka:plugin-templating:${project.version}")
    devPublication("org.jetbrains.dokka:plugin-versioning:${project.version}")

    devPublication(project)
}

gradlePlugin {
    isAutomatedPublishing = true

    plugins.register("dokka") {
        id = "org.jetbrains.dokka"
        displayName = "Dokka Gradle Plugin"
        description = "Dokka is an API documentation engine for Kotlin"
        implementationClass = "org.jetbrains.dokka.gradle.DokkaPlugin"
    }

    plugins.configureEach {
        website.set("https://kotlinlang.org/docs/dokka-introduction.html")
        vcsUrl.set("https://github.com/Kotlin/dokka.git")
        tags.addAll(
            "dokka",
            "kotlin",
            "kdoc",
            "android",
            "api reference",
            "documentation",
            "html",
            "website",
        )
    }
}

testing.suites {
    withType<JvmTestSuite>().configureEach {
        useJUnitJupiter()

        dependencies {
            implementation(gradleTestKit())

            implementation(testFixtures(project()))

            implementation(platform(libs.kotlinxSerialization.bom))
            implementation(libs.kotlinxSerialization.json)

            //region classic-plugin dependencies - delete these when src/classicMain is removed
            implementation(libs.kotlin.test)
            implementation(libs.gradlePlugin.kotlin)
            implementation(libs.gradlePlugin.kotlin.klibCommonizerApi)
            implementation(libs.gradlePlugin.android)
            implementation("org.jetbrains.dokka:dokka-test-api:${project.version}")
            //endregion
        }

        targets.configureEach {
            testTask.configure {
                devMavenPublish.configureTask(this)

                val projectTestTempDirPath = layout.buildDirectory.dir("test-temp-dir").get().asFile
                inputs.property("projectTestTempDir", projectTestTempDirPath)
                systemProperty("projectTestTempDir", projectTestTempDirPath)

                systemProperty("kotest.framework.config.fqn", "org.jetbrains.dokka.gradle.utils.KotestProjectConfig")
                // FIXME remove autoscan when Kotest >= 6.0
                systemProperty("kotest.framework.classpath.scanning.autoscan.disable", "true")
            }
        }
    }


    /** Unit tests suite */
    val test by getting(JvmTestSuite::class) {
        description = "Standard unit tests"
    }


    /** Functional tests suite */
    val testFunctional by registering(JvmTestSuite::class) {
        description = "Tests that use Gradle TestKit to test functionality"
        testType.set(TestSuiteType.FUNCTIONAL_TEST)

        targets.all {
            testTask.configure {
                shouldRunAfter(test)
            }
        }

        dependencies {
            implementation(project.dependencies.platform(libs.ktor.bom))
            implementation(libs.ktorServer.core)
            implementation(libs.ktorServer.cio)
        }
    }

    tasks.check { dependsOn(test, testFunctional) }
}

skipTestFixturesPublications()

apiValidation {
    nonPublicMarkers.add("org.jetbrains.dokka.gradle.internal.DokkaInternalApi")
}

val generateDokkaGradlePluginConstants by tasks.registering(GenerateDokkaGradlePluginConstants::class) {
    val dokkaPluginConstants = objects.mapProperty<String, String>().apply {
        put("DOKKA_VERSION", project.version.toString())
        put("DOKKA_DEPENDENCY_VERSION_KOTLINX_HTML", libs.versions.kotlinx.html)
        put("DOKKA_DEPENDENCY_VERSION_KOTLINX_COROUTINES", libs.versions.kotlinx.coroutines)
        put("DOKKA_DEPENDENCY_VERSION_FREEMARKER", libs.versions.freemarker)
        put("DOKKA_DEPENDENCY_VERSION_JETBRAINS_MARKDOWN", libs.versions.jetbrains.markdown)
    }

    properties.set(dokkaPluginConstants)
    destinationDir.set(layout.buildDirectory.dir("generated-source/main/kotlin/"))
}


kotlin {
    sourceSets {
        main {
            kotlin.srcDir(generateDokkaGradlePluginConstants)
        }
    }
}
