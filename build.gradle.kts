/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.base")
}

val publishedIncludedBuilds = listOf("runner-cli", "runner-gradle-classic", "runner-maven")

addDependencyOnSameTaskOfIncludedBuilds("assemble")
addDependencyOnSameTaskOfIncludedBuilds("build")
addDependencyOnSameTaskOfIncludedBuilds("clean")
addDependencyOnSameTaskOfIncludedBuilds("check")

registerParentTaskOfIncludedBuilds("test", groupName = "verification")

registerParentTaskOfPublishedIncludedBuilds("apiCheck", groupName = "verification")
registerParentTaskOfPublishedIncludedBuilds("apiDump", groupName = "other")

registerParentTaskOfPublishedIncludedBuilds("publishAllPublicationsToMavenCentralRepository", groupName = "publication")
registerParentTaskOfPublishedIncludedBuilds("publishAllPublicationsToProjectLocalRepository", groupName = "publication")
registerParentTaskOfPublishedIncludedBuilds("publishAllPublicationsToSnapshotRepository", groupName = "publication")
registerParentTaskOfPublishedIncludedBuilds("publishAllPublicationsToSpaceDevRepository", groupName = "publication")
registerParentTaskOfPublishedIncludedBuilds("publishAllPublicationsToSpaceTestRepository", groupName = "publication")
registerParentTaskOfPublishedIncludedBuilds("publishToMavenLocal", groupName = "publication")

registerParentTaskOfIncludedBuilds("publishPlugins", groupName = "publication") {
    it.name == "runner-gradle-classic"
}

tasks.register("integrationTest") {
    group = "verification"
    description = "Runs integration tests of this project. Might take a while and require additional setup."
    dependsOn(gradle.includedBuilds.single { it.name == "dokka-integration-tests" }.task(":integrationTest"))
}

fun addDependencyOnSameTaskOfIncludedBuilds(existingTaskName: String, filter: (IncludedBuild) -> Boolean = { true }) {
    tasks.named(existingTaskName) {
        dependsOn(includedBuildTasks(existingTaskName, filter))
    }
}

fun registerParentTaskOfPublishedIncludedBuilds(
    taskName: String,
    groupName: String
) = registerParentTaskOfIncludedBuilds(taskName, groupName) {
    it.name in publishedIncludedBuilds
}

fun registerParentTaskOfIncludedBuilds(
    taskName: String,
    groupName: String,
    filter: (IncludedBuild) -> Boolean = { true }
) {
    tasks.register(taskName) {
        group = groupName
        description = "Runs $taskName tasks of included builds"
        dependsOn(includedBuildTasks(taskName, filter))
    }
}

fun includedBuildTasks(taskName: String, filter: (IncludedBuild) -> Boolean): List<TaskReference> =
    gradle.includedBuilds.filter { it.name != "build-logic" }.filter(filter).map { it.task(":$taskName") }
