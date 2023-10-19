/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.conventions.base")
}

val dokka_version: String by project

group = "org.jetbrains.dokka"
version = dokka_version

registerParentTask("test", groupName = "verification")

registerParentTask("publishAllPublicationsToMavenCentralRepository", groupName = "publication")
registerParentTask("publishAllPublicationsToProjectLocalRepository", groupName = "publication")
registerParentTask("publishAllPublicationsToSnapshotRepository", groupName = "publication")
registerParentTask("publishAllPublicationsToSpaceDevRepository", groupName = "publication")
registerParentTask("publishAllPublicationsToSpaceTestRepository", groupName = "publication")
registerParentTask("publishToMavenLocal", groupName = "publication")

addDependencyToSubprojectTasks("assemble")
addDependencyToSubprojectTasks("build")
addDependencyToSubprojectTasks("clean")
addDependencyToSubprojectTasks("check")

fun addDependencyToSubprojectTasks(existingTaskName: String) {
    tasks.named(existingTaskName) {
        val subprojectTasks = subprojects
            .filter { it.getTasksByName(existingTaskName, false).isNotEmpty() }
            .map { ":${it.name}:$existingTaskName" }

        dependsOn(subprojectTasks)
    }
}

fun registerParentTask(taskName: String, groupName: String) {
    tasks.register(taskName) {
        group = groupName
        description = "Runs $taskName tasks of all subprojects"

        val subprojectTasks = subprojects
            .filter { it.getTasksByName(taskName, false).isNotEmpty() }
            .map { ":${it.name}:$taskName" }

        dependsOn(subprojectTasks)
    }
}
