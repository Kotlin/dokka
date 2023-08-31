/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.jetbrains.conventions.dokka-integration-test")
    id("com.github.johnrengelman.shadow")
}

val dokka_version: String by project
evaluationDependsOn(":runners:cli")
evaluationDependsOn(":plugins:base")

dependencies {
    implementation(kotlin("test-junit5"))
    implementation(projects.integrationTests)
}

/* Create a fat base plugin jar for cli tests */
val basePluginShadow: Configuration by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, "java-runtime"))
    }
}

dependencies {
    basePluginShadow(projects.plugins.base)

    // TODO [beresnev] analysis switcher
    basePluginShadow(project(path = ":subprojects:analysis-kotlin-descriptors", configuration = "shadow"))
}

val basePluginShadowJar by tasks.register("basePluginShadowJar", ShadowJar::class) {
    configurations = listOf(basePluginShadow)
    archiveFileName.set("fat-base-plugin-$dokka_version.jar")
    archiveClassifier.set("")

    // service files are merged to make sure all Dokka plugins
    // from the dependencies are loaded, and not just a single one.
    mergeServiceFiles()
}

tasks.integrationTest {
    inputs.dir(file("projects"))
    val cliJar = tasks.getByPath(":runners:cli:shadowJar") as ShadowJar
    environment("CLI_JAR_PATH", cliJar.archiveFile.get())
    environment("BASE_PLUGIN_JAR_PATH", basePluginShadowJar.archiveFile.get())
    dependsOn(cliJar)
    dependsOn(basePluginShadowJar)
}
