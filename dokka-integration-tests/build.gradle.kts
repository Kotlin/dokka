/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.base")
}

addDependencyToSubprojectTasks("assemble")
addDependencyToSubprojectTasks("build")
addDependencyToSubprojectTasks("clean")
addDependencyToSubprojectTasks("check")

registerParentTask("test", groupName = "verification")
registerParentTask("integrationTest", groupName = "verification")

fun addDependencyToSubprojectTasks(existingTaskName: String) {
    tasks.named(existingTaskName) {
        dependsOn(subprojectTasks(existingTaskName))
    }
}

fun registerParentTask(taskName: String, groupName: String) {
    tasks.register(taskName) {
        group = groupName
        description = "Runs $taskName tasks of all subprojects"
        dependsOn(subprojectTasks(taskName))
    }
}

fun subprojectTasks(taskName: String): List<String> =
    subprojects
        .filter { it.getTasksByName(taskName, false).isNotEmpty() }
        .map { ":${it.name}:$taskName" }
