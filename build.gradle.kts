/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.base")
}

val publishedIncludedBuilds = listOf("runner-cli", "runner-gradle-plugin-classic", "runner-maven-plugin")
val gradlePluginIncludedBuilds = listOf("runner-gradle-plugin-classic")

addDependencyOnSameTasksOfIncludedBuilds("assemble", "build", "clean", "check")

registerParentGroupTasks(
    "publishing", taskNames = listOf(
        "publishAllPublicationsToMavenCentralRepository",
        "publishAllPublicationsToProjectLocalRepository",
        "publishAllPublicationsToSnapshotRepository",
        "publishAllPublicationsToSpaceDevRepository",
        "publishAllPublicationsToSpaceTestRepository",
        "publishToMavenLocal"
    )
) {
    it.name in publishedIncludedBuilds
}

registerParentGroupTasks(
    "gradle plugin", taskNames = listOf(
        "publishPlugins",
        "validatePlugins"
    )
) {
    it.name in gradlePluginIncludedBuilds
}

registerParentGroupTasks(
    "bcv", taskNames = listOf(
        "apiDump",
        "apiCheck",
        "apiBuild"
    )
) {
    it.name in publishedIncludedBuilds
}

registerParentGroupTasks(
    "verification", taskNames = listOf(
        "test"
    )
)

tasks.register("integrationTest") {
    group = "verification"
    description = "Runs integration tests of this project. Might take a while and require additional setup."

    dependsOn(includedBuildTasks("integrationTest") {
        it.name == "dokka-integration-tests"
    })
}

fun addDependencyOnSameTasksOfIncludedBuilds(vararg taskNames: String) {
    taskNames.forEach { taskName ->
        tasks.named(taskName) {
            dependsOn(includedBuildTasks(taskName))
        }
    }
}

fun registerParentGroupTasks(
    groupName: String,
    taskNames: List<String>,
    includedBuildFilter: (IncludedBuild) -> Boolean = { true }
) = taskNames.forEach { taskName ->
    tasks.register(taskName) {
        group = groupName
        description = "A parent task that calls tasks with the same name in all subprojects and included builds"

        dependsOn(subprojectTasks(taskName), includedBuildTasks(taskName, includedBuildFilter))
    }
}

fun subprojectTasks(taskName: String): List<String> =
    subprojects
        .filter { it.getTasksByName(taskName, false).isNotEmpty() }
        .map { ":${it.path}:$taskName" }


fun includedBuildTasks(taskName: String, filter: (IncludedBuild) -> Boolean = { true }): List<TaskReference> =
    gradle.includedBuilds
        .filter { it.name != "build-logic" }
        .filter(filter)
        .mapNotNull { it.task(":$taskName") }


tasks.wrapper {
    // Gradle requires that checksum is defined both in the wrapper task *and* in gradle-wrapper.properties
    // Checksums are available here: https://gradle.org/release-checksums/
    distributionSha256Sum = "544c35d6bd849ae8a5ed0bcea39ba677dc40f49df7d1835561582da2009b961d"
    doLast {
        // Manually update the distribution URL to use cache-redirector.
        // (Workaround for https://github.com/gradle/gradle/issues/17515)
        propertiesFile.writeText(
            propertiesFile.readText()
                .replace(
                    "https\\://services.gradle.org/",
                    "https\\://cache-redirector.jetbrains.com/services.gradle.org/",
                )
        )
    }
}
