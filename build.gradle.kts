/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import dokkabuild.TaskPathCollector.Companion.dependsOnIncludedBuildTasks
import org.gradle.api.publish.plugins.PublishingPlugin.PUBLISH_TASK_GROUP
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP

plugins {
    id("dokkabuild.base")
}


//region Workarounds for running all tasks in included builds
// https://github.com/gradle/gradle/issues/22335

// Setup lifecycle tasks dependencies, so each is propagated to included builds.
tasks.assemble {
    dependsOnIncludedBuildTasks()
}

tasks.build {
    dependsOnIncludedBuildTasks()
}

tasks.clean {
    dependsOnIncludedBuildTasks()
}

tasks.check {
    dependsOnIncludedBuildTasks()
}

val publishPlugins by tasks.registering {
    group = "gradle plugin"
    dependsOnIncludedBuildTasks()
}

val validatePlugins by tasks.registering {
    group = "gradle plugin"
    dependsOnIncludedBuildTasks()
}

val apiDump by tasks.registering {
    group = "$VERIFICATION_GROUP bcv"
    dependsOnIncludedBuildTasks()
}

val apiCheck by tasks.registering {
    group = "$VERIFICATION_GROUP bcv"
    dependsOnIncludedBuildTasks()
}

val test by tasks.registering {
    group = VERIFICATION_GROUP
    dependsOnIncludedBuildTasks()
    dependsOn(apiCheck)
}

val integrationTest by tasks.registering {
    group = VERIFICATION_GROUP
    dependsOnIncludedBuildTasks()
    description = "Runs integration tests of this project. Might take a while and require additional setup."
}

val publishAllPublicationsToRemoteRepositories by tasks.registering {
    group = PUBLISH_TASK_GROUP
    dependsOnIncludedBuildTasks("publishAllPublicationsToMavenCentralRepository")
    dependsOnIncludedBuildTasks("publishAllPublicationsToProjectLocalRepository")
    dependsOnIncludedBuildTasks("publishAllPublicationsToSnapshotRepository")
    dependsOnIncludedBuildTasks("publishAllPublicationsToSpaceDevRepository")
}
val publishAllPublicationsToMavenCentralRepository by tasks.registering {
    group = PUBLISH_TASK_GROUP
    dependsOnIncludedBuildTasks()
}
val publishAllPublicationsToProjectLocalRepository by tasks.registering {
    group = PUBLISH_TASK_GROUP
    dependsOnIncludedBuildTasks()
}
val publishAllPublicationsToSnapshotRepository by tasks.registering {
    group = PUBLISH_TASK_GROUP
    dependsOnIncludedBuildTasks()
}
val publishAllPublicationsToSpaceDevRepository by tasks.registering {
    group = PUBLISH_TASK_GROUP
    dependsOnIncludedBuildTasks()
}
val publishAllPublicationsToSpaceTestRepository by tasks.registering {
    group = PUBLISH_TASK_GROUP
    dependsOnIncludedBuildTasks()
}
val publishToMavenLocal by tasks.registering {
    group = PUBLISH_TASK_GROUP
    dependsOnIncludedBuildTasks()
}
//endregion


tasks.wrapper {
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
