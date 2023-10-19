/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UnstableApiUsage") // jvm test suites & test report aggregation are incubating

import buildsrc.utils.skipTestFixturesPublications

plugins {
    buildsrc.conventions.`kotlin-gradle-plugin`
    kotlin("plugin.serialization")

    `java-test-fixtures`
    `jvm-test-suite`
    `test-report-aggregation`
}

description = "Generates documentation for Kotlin projects (using Dokka)"

dependencies {
    // ideally there should be a 'dokka-core-api' dependency (that is very thin and doesn't drag in loads of unnecessary code)
    // that would be used as an implementation dependency, while dokka-core would be used as a compileOnly dependency
    // https://github.com/Kotlin/dokka/issues/2933
    implementation(libs.dokka.core)

    compileOnly(libs.gradlePlugin.kotlin)
    compileOnly(libs.gradlePlugin.kotlin.klibCommonizerApi)
    compileOnly(libs.gradlePlugin.android.dokkatoo)
    compileOnly(libs.gradlePlugin.androidApi.dokkatoo)

    implementation(platform(libs.kotlinx.serialization.bom))
    implementation(libs.kotlinx.serialization.json)

    testFixturesImplementation(gradleApi())
    testFixturesImplementation(gradleTestKit())

    testFixturesCompileOnly(libs.dokka.core)
    testFixturesImplementation(platform(libs.kotlinx.serialization.bom))
    testFixturesImplementation(libs.kotlinx.serialization.json)

    testFixturesCompileOnly(libs.dokka.core)

    testFixturesApi(platform(libs.kotest.bom))
    testFixturesApi(libs.kotest.junit5Runner)
    testFixturesApi(libs.kotest.assertionsCore)
    testFixturesApi(libs.kotest.assertionsJson)
    testFixturesApi(libs.kotest.datatest)

    // don't define test dependencies here, instead define them in the testing.suites {} configuration below
}

// TODO [structure-refactoring] change / extract?
gradlePlugin {
    isAutomatedPublishing = true

    plugins.register("dokkatoo") {
        id = "org.jetbrains.dokka.dokkatoo"
        displayName = "Dokkatoo"
        description = "Generates documentation for Kotlin projects (using Dokka)"
        implementationClass = "org.jetbrains.dokka.dokkatoo.DokkatooPlugin"
    }

    fun registerDokkaPlugin(
        pluginClass: String,
        shortName: String,
        longName: String = shortName,
    ) {
        plugins.register(pluginClass) {
            id = "org.jetbrains.dokka.dokkatoo-${shortName.toLowerCase()}"
            displayName = "Dokkatoo $shortName"
            description = "Generates $longName documentation for Kotlin projects (using Dokka)"
            implementationClass = "org.jetbrains.dokka.dokkatoo.formats.$pluginClass"
        }
    }
    registerDokkaPlugin("DokkatooGfmPlugin", "GFM", longName = "GFM (GitHub Flavoured Markdown)")
    registerDokkaPlugin("DokkatooHtmlPlugin", "HTML")
    registerDokkaPlugin("DokkatooJavadocPlugin", "Javadoc")
    registerDokkaPlugin("DokkatooJekyllPlugin", "Jekyll")

    plugins.configureEach {
        website.set("https://github.com/adamko-dev/dokkatoo/")
        vcsUrl.set("https://github.com/adamko-dev/dokkatoo.git")
        tags.addAll(
            "dokka",
            "dokkatoo",
            "kotlin",
            "kdoc",
            "android",
            "documentation",
            "javadoc",
            "html",
            "markdown",
            "gfm",
            "website",
        )
    }
}

kotlin {
    target {
        compilations.configureEach {
            // TODO Dokkatoo uses Gradle 8, while Dokka uses Gradle 7, which has an older version of Kotlin that
            //      doesn't include these options - so update them or update Gradle.
//      compilerOptions.configure {
//        freeCompilerArgs.addAll(
//          "-opt-in=org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi",
//        )
//      }
        }
    }
}

skipTestFixturesPublications()

val dokkatooVersion = provider { project.version.toString() }

val dokkatooConstantsProperties = objects.mapProperty<String, String>().apply {
    put("DOKKATOO_VERSION", dokkatooVersion)
    put("DOKKA_VERSION", libs.versions.gradlePlugin.dokka)
}

val buildConfigFileContents: Provider<TextResource> =
    dokkatooConstantsProperties.map { constants ->

        val vals = constants.entries
            .sortedBy { it.key }
            .joinToString("\n") { (k, v) ->
                """const val $k = "$v""""
            }.prependIndent("  ")

        resources.text.fromString(
            """
        |package org.jetbrains.dokka.dokkatoo.internal
        |
        |@DokkatooInternalApi
        |object DokkatooConstants {
        |$vals
        |}
        |
      """.trimMargin()
        )
    }

val generateDokkatooConstants by tasks.registering(Sync::class) {
    group = project.name

    val buildConfigFileContents = buildConfigFileContents

    from(buildConfigFileContents) {
        rename { "DokkatooConstants.kt" }
        into("dev/adamko/dokkatoo/internal/")
    }

    into(layout.buildDirectory.dir("generated-source/main/kotlin/"))
}

kotlin.sourceSets.main {
    kotlin.srcDir(generateDokkatooConstants.map { it.destinationDir })
}
