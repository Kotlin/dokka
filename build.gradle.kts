/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@Suppress("DSL_SCOPE_VIOLATION") // fixed in Gradle 8.1 https://github.com/gradle/gradle/pull/23639
plugins {
    base
}

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

// TODO [structure-refactoring] - only for gradle plugins
//registerParentTaskOfIncludedBuilds("publishPlugins", groupName = "publication")

fun addDependencyOnSameTaskOfIncludedBuilds(existingTaskName: String) {
    tasks.named(existingTaskName) {
        dependsOn(includedBuildTasks(existingTaskName))
    }
}

fun registerParentTaskOfIncludedBuilds(taskName: String, groupName: String) {
    tasks.register(taskName) {
        group = groupName
        description = "Runs $taskName tasks of all included builds"
        dependsOn(includedBuildTasks(taskName))
    }
}

fun includedBuildTasks(taskName: String): List<TaskReference> =
    gradle.includedBuilds.filter { it.name != "build-logic" }.map { it.task(":$taskName") }
