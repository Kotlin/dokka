/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.conventions.base")
}

addDependencyOnSameTaskOfIncludedBuilds("assemble")
addDependencyOnSameTaskOfIncludedBuilds("build")
addDependencyOnSameTaskOfIncludedBuilds("clean")
addDependencyOnSameTaskOfIncludedBuilds("check")

registerParentTaskOfIncludedBuilds("test", groupName = "verification")
registerParentTaskOfIncludedBuilds("apiCheck", groupName = "verification") { it.name != "dokka-integration-tests" }
registerParentTaskOfIncludedBuilds("apiDump", groupName = "other") { it.name != "dokka-integration-tests" }

registerParentTaskOfIncludedBuilds("publishAllPublicationsToMavenCentralRepository", groupName = "publication")
registerParentTaskOfIncludedBuilds("publishAllPublicationsToProjectLocalRepository", groupName = "publication")
registerParentTaskOfIncludedBuilds("publishAllPublicationsToSnapshotRepository", groupName = "publication")
registerParentTaskOfIncludedBuilds("publishAllPublicationsToSpaceDevRepository", groupName = "publication")
registerParentTaskOfIncludedBuilds("publishAllPublicationsToSpaceTestRepository", groupName = "publication")
registerParentTaskOfIncludedBuilds("publishToMavenLocal", groupName = "publication")

// TODO [structure-refactoring] - only for gradle plugins
//registerParentTaskOfIncludedBuilds("publishPlugins", groupName = "publication")

fun addDependencyOnSameTaskOfIncludedBuilds(existingTaskName: String, filter: (IncludedBuild) -> Boolean = { true }) {
    tasks.named(existingTaskName) {
        dependsOn(includedBuildTasks(existingTaskName, filter))
    }
}

fun registerParentTaskOfIncludedBuilds(
    taskName: String,
    groupName: String,
    filter: (IncludedBuild) -> Boolean = { true }
) {
    tasks.register(taskName) {
        group = groupName
        description = "Runs $taskName tasks of all included builds"
        dependsOn(includedBuildTasks(taskName, filter))
    }
}

fun includedBuildTasks(taskName: String, filter: (IncludedBuild) -> Boolean): List<TaskReference> =
    gradle.includedBuilds.filter { it.name != "build-logic" }.filter(filter).map { it.task(":$taskName") }
