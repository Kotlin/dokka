/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.ValidatePublications
import org.jetbrains.publicationChannels

@Suppress("DSL_SCOPE_VIOLATION") // fixed in Gradle 8.1 https://github.com/gradle/gradle/pull/23639
plugins {
    id("org.jetbrains.conventions.base")
    id("org.jetbrains.conventions.dokka")

// TODO [structure-refactoring] enable
//    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
    alias(libs.plugins.nexusPublish)
}

val dokka_version: String by project

group = "org.jetbrains.dokka"
version = dokka_version


logger.lifecycle("Publication version: $dokka_version")
tasks.register<ValidatePublications>("validatePublications")

nexusPublishing {
    repositories {
        sonatype {
            username.set(System.getenv("SONATYPE_USER"))
            password.set(System.getenv("SONATYPE_PASSWORD"))
        }
    }
}

val dokkaPublish by tasks.registering {
    if (publicationChannels.any { it.isMavenRepository() }) {
        finalizedBy(tasks.named("closeAndReleaseSonatypeStagingRepository"))
    }
}

addDependencyOnSameTaskOfIncludedBuilds("assemble")
addDependencyOnSameTaskOfIncludedBuilds("build")
addDependencyOnSameTaskOfIncludedBuilds("clean")
addDependencyOnSameTaskOfIncludedBuilds("check")

registerParentTaskOfIncludedBuilds("test", groupName = "verification")

fun addDependencyOnSameTaskOfIncludedBuilds(existingTaskName: String) {
    tasks.named(existingTaskName) {
        dependsOn(gradle.includedBuilds.map { it.task(":$existingTaskName") })
    }
}

fun registerParentTaskOfIncludedBuilds(taskName: String, groupName: String) {
    tasks.register(taskName) {
        group = groupName
        description = "Runs $taskName tasks of all included builds"
        dependsOn(gradle.includedBuilds.map { it.task(":$taskName") })
    }
}

//apiValidation {
//    // note that subprojects are ignored by their name, not their path https://github.com/Kotlin/binary-compatibility-validator/issues/16
//    ignoredProjects += setOf(
//        // NAME                    PATH
//        "frontend",            // :plugins:base:frontend
//
//        "integration-tests",   // :integration-tests
//        "gradle",              // :integration-tests:gradle
//        "cli",                 // :integration-tests:cli
//        "maven",               // integration-tests:maven
//    )
//}
