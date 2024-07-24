@file:Suppress("UnstableApiUsage") // jvm test suites

import dokkabuild.tasks.GenerateDokkatooConstants
import dokkabuild.utils.skipTestFixturesPublications
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("dokkabuild.base")

    `kotlin-dsl`
    kotlin("plugin.serialization") version embeddedKotlinVersion

    id("dokkabuild.dev-maven-publish")
    id("dev.adamko.kotlin.binary-compatibility-validator") version "0.1.0"
    `java-test-fixtures`
    `jvm-test-suite`

    id("dokkabuild.publish-base")
    alias(libs.plugins.gradlePluginPublish)
}

val dokkaVersion = "2.0.20-SNAPSHOT"
version = "2.0.20-SNAPSHOT"
group = "org.jetbrains.dokka"
description = "Generates documentation for Kotlin projects (using Dokka)"

kotlin {
    jvmToolchain(8)

    compilerOptions {
        @Suppress("DEPRECATION")
        languageVersion = KotlinVersion.KOTLIN_1_4
        apiVersion = languageVersion
    }

    sourceSets {
        configureEach {
            languageSettings {
                optIn("org.jetbrains.dokka.gradle.internal.DokkatooInternalApi")
                optIn("kotlin.io.path.ExperimentalPathApi")
            }
        }

        main {
            kotlin.srcDir("src/classicMain/kotlin")
        }
        test {
            kotlin.srcDir("src/classicTest/kotlin")
        }
    }
}

dependencies {
    // ideally there should be a 'dokka-core-api' dependency (that is very thin and doesn't drag in loads of unnecessary code)
    // that would be used as an implementation dependency, while dokka-core would be used as a compileOnly dependency
    // https://github.com/Kotlin/dokka/issues/2933
    implementation("org.jetbrains.dokka:dokka-core:$dokkaVersion")

    compileOnly(libs.gradlePlugin.kotlin)
    compileOnly(libs.gradlePlugin.kotlin.klibCommonizerApi)
    compileOnly(libs.gradlePlugin.android)
    compileOnly(libs.gradlePlugin.androidApi)

    implementation(platform(libs.kotlinxSerialization.bom))
    implementation(libs.kotlinxSerialization.json)

    testFixturesImplementation(gradleApi())
    testFixturesImplementation(gradleTestKit())

    testFixturesCompileOnly("org.jetbrains.dokka:dokka-core:$dokkaVersion")
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

    devPublication(project)
}

gradlePlugin {
    isAutomatedPublishing = true

    plugins.register("dokkatoo") {
        id = "dev.adamko.dokkatoo"
        displayName = "Dokka"
        description = "Dokka is an API documentation engine for Kotlin"
        implementationClass = "org.jetbrains.dokka.gradle.DokkatooPlugin"
    }

    plugins.register("dokka") {
        id = "org.jetbrains.dokka"
        displayName = "Dokka"
        description = "Dokka is an API documentation engine for Kotlin"
        implementationClass = "org.jetbrains.dokka.gradle.DokkatooPlugin"
    }

    fun registerDokkaPlugin(
        pluginClass: String,
        shortName: String,
        longName: String = shortName,
    ) {
        plugins.register(pluginClass) {
            id = "dev.adamko.dokkatoo-${shortName.lowercase()}"
            displayName = "Dokkatoo $shortName"
            description = "Generates $longName documentation for Kotlin projects (using Dokka)"
            implementationClass = "org.jetbrains.dokka.gradle.formats.$pluginClass"
        }
    }
    registerDokkaPlugin("DokkatooGfmPlugin", "GFM", longName = "GFM (GitHub Flavoured Markdown)")
    registerDokkaPlugin("DokkatooHtmlPlugin", "HTML")
    registerDokkaPlugin("DokkatooJavadocPlugin", "Javadoc")
    registerDokkaPlugin("DokkatooJekyllPlugin", "Jekyll")

    plugins.configureEach {
//        website.set("https://adamko-dev.github.io/dokkatoo/")
//        vcsUrl.set("https://github.com/adamko-dev/dokkatoo.git")
        tags.addAll(
            "dokka",
//            "dokkatoo",
            "kotlin",
            "kdoc",
            "android",
            "api reference",
            "documentation",
//            "javadoc",
            "html",
//            "markdown",
//            "gfm",
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

            //region classic-plugin dependencies
            implementation(libs.kotlin.test)
            implementation(libs.gradlePlugin.kotlin)
            implementation(libs.gradlePlugin.kotlin.klibCommonizerApi)
            implementation(libs.gradlePlugin.android)
            implementation("org.jetbrains.dokka:dokka-test-api:$dokkaVersion")
            //endregion
        }

        targets.configureEach {
            testTask.configure {
                devMavenPublish.configureTask(this)

                val projectTestTempDirPath = layout.buildDirectory.dir("test-temp-dir").get().asFile
                inputs.property("projectTestTempDir", projectTestTempDirPath)
                systemProperty("projectTestTempDir", projectTestTempDirPath)

//                when (testType.get()) {
//                    TestSuiteType.FUNCTIONAL_TEST,
//                    TestSuiteType.INTEGRATION_TEST -> {
//                        dependsOn(tasks.matching { it.name == "publishAllPublicationsToTestRepository" })
//                    }
//                }

                systemProperty("kotest.framework.config.fqn", "org.jetbrains.dokka.gradle.utils.KotestProjectConfig")
                // FIXME remove when Kotest >= 6.0
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

binaryCompatibilityValidator {
    ignoredMarkers.add("org.jetbrains.dokka.gradle.internal.DokkatooInternalApi")
}

val generateDokkatooConstants by tasks.registering(GenerateDokkatooConstants::class) {
    val dokkaPluginConstants = objects.mapProperty<String, String>().apply {
        // TODO remove DOKKATOO_VERSION
        put("DOKKATOO_VERSION", dokkaVersion)
//        put("DOKKA_VERSION", dokkaBuild.projectVersion)
        put("DOKKA_VERSION", dokkaVersion) // TODO how to get actual Dokka project version?
        put("DOKKA_DEPENDENCY_VERSION_KOTLINX_HTML", libs.versions.kotlinx.html)
        put("DOKKA_DEPENDENCY_VERSION_KOTLINX_COROUTINES", libs.versions.kotlinx.coroutines)
        put("DOKKA_DEPENDENCY_VERSION_FREEMARKER", libs.versions.freemarker)
        put("DOKKA_DEPENDENCY_VERSION_JETBRAINS_MARKDOWN", libs.versions.jetbrains.markdown)
    }

    properties.set(dokkaPluginConstants)
    destinationDir.set(layout.buildDirectory.dir("generated-source/main/kotlin/"))
//  dokkaSource.fileProvider(tasks.prepareDokkaSource.map { it.destinationDir })
}


kotlin {
    sourceSets {
        main {
            kotlin.srcDir(generateDokkatooConstants)
        }
    }
}

//dokkatoo {
//  moduleName = "Dokkatoo Gradle Plugin"
//
//  dokkatooSourceSets.configureEach {
//    includes.from("Module.md")
//  }
//}
