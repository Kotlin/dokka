/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@Suppress("DSL_SCOPE_VIOLATION") // fixed in Gradle 8.1 https://github.com/gradle/gradle/pull/23639
plugins {
    id("org.jetbrains.conventions.base")
    id("org.jetbrains.conventions.dokka")
}

val dokka_version: String by project

group = "org.jetbrains.dokka"
version = dokka_version

addDependencyOnSameTaskOfIncludedBuilds("assemble")
addDependencyOnSameTaskOfIncludedBuilds("build")
addDependencyOnSameTaskOfIncludedBuilds("clean")
addDependencyOnSameTaskOfIncludedBuilds("check")

registerParentTaskOfIncludedBuilds("test", groupName = "verification")

registerParentTaskOfIncludedBuilds("publishAllPublicationsToMavenCentralRepository", groupName = "publication")
registerParentTaskOfIncludedBuilds("publishAllPublicationsToProjectLocalRepository", groupName = "publication")
registerParentTaskOfIncludedBuilds("publishAllPublicationsToSnapshotRepository", groupName = "publication")
registerParentTaskOfIncludedBuilds("publishAllPublicationsToSpaceDevRepository", groupName = "publication")
registerParentTaskOfIncludedBuilds("publishAllPublicationsToSpaceTestRepository", groupName = "publication")
registerParentTaskOfIncludedBuilds("publishToMavenLocal", groupName = "publication")


fun addDependencyOnSameTaskOfIncludedBuilds(existingTaskName: String) {
    tasks.named(existingTaskName) {
        dependsOn(gradle.includedBuilds.filter { it.name != "build-logic" }.map { it.task(":$existingTaskName") })
    }
}

fun registerParentTaskOfIncludedBuilds(taskName: String, groupName: String) {
    tasks.register(taskName) {
        group = groupName
        description = "Runs $taskName tasks of all included builds"
        dependsOn(gradle.includedBuilds.filter { it.name != "build-logic" }.map { it.task(":$taskName") })
    }
}
