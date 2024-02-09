/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

// root build.gradle.kts

plugins {
    id("org.jetbrains.dokka") version "1.9.10" apply false
}

allprojects {
    plugins.apply("org.jetbrains.dokka")

    val dokkaPlugin by configurations
    dependencies {
        dokkaPlugin("org.jetbrains.dokka:versioning-plugin:1.9.10")
    }
}

val dokkaOutputDir = "../versions"

tasks.withType<DokkaMultiModuleTask> {
    val id = "org.jetbrains.dokka.versioning.VersioningPlugin"
    val config = """{ "version": "$configuredVersion", "olderVersionsDir":"$dokkaOutputDir" }"""
    val mapOf = mapOf(id to config)

    outputDirectory.set(file(projectDir.toPath().resolve(dokkaOutputDir).resolve(configuredVersion)))
    pluginsMapConfiguration.set(mapOf)
}
