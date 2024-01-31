/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("dokkabuild.test-integration")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(kotlin("test-junit5"))
    implementation(libs.junit.jupiterApi)
    implementation(projects.utilities)
}

val cliPluginsClasspath: Configuration by configurations.creating {
    description = "plugins/dependencies required to run CLI with base plugin"
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
    }

    // we don't fetch transitive dependencies here to be able to control external dependencies explicitly
    isTransitive = false
}

val cliClasspath: Configuration by configurations.creating {
    description = "dependency on CLI JAR"
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
        attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling.SHADOWED))
    }
    // we should have single artifact here
    isTransitive = false
}

dependencies {
    cliClasspath("org.jetbrains.dokka:runner-cli")

    cliPluginsClasspath("org.jetbrains.dokka:plugin-base")
    // required dependencies of `plugin-base`
    cliPluginsClasspath(libs.freemarker)
    cliPluginsClasspath(libs.kotlinx.html)

    val tryK2 = project.providers
        .gradleProperty("org.jetbrains.dokka.experimental.tryK2")
        .map(String::toBoolean)
        .orNull ?: false

    val analysisDependency = when {
        tryK2 -> "org.jetbrains.dokka:analysis-kotlin-symbols"
        else -> "org.jetbrains.dokka:analysis-kotlin-descriptors"
    }

    cliPluginsClasspath(analysisDependency) {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling.SHADOWED))
        }
    }
}

val cliPluginsShadowJar by tasks.registering(ShadowJar::class) {
    archiveFileName.set("cli-plugins-${project.version}.jar")
    configurations = listOf(cliPluginsClasspath)

    // service files are merged to make sure all Dokka plugins
    // from the dependencies are loaded, and not just a single one.
    mergeServiceFiles()
}

tasks.integrationTest {
    dependsOn(cliClasspath)
    dependsOn(cliPluginsShadowJar)

    inputs.dir(file("projects"))
    environment("CLI_JAR_PATH", cliClasspath.singleFile)
    environment("BASE_PLUGIN_JAR_PATH", cliPluginsShadowJar.get().archiveFile.get())
}
