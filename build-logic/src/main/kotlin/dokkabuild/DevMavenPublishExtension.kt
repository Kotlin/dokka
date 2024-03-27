/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package dokkabuild

import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.process.JavaForkOptions

abstract class DevMavenPublishExtension(
    /**
     * Resolves Dev Maven repos from the current project's `devPublication` dependencies.
     *
     * Must only contain directories.
     */
    private val devMavenRepositories: FileCollection,
) {

    /**
     * Files suitable for registering as a task input (as in, the files are reproducible-build compatible).
     */
    private val devMavenRepositoriesInputFiles: FileTree = devMavenRepositories
        .asFileTree
        .matching {
            // Exclude Maven Metadata files because they contain timestamps, meaning tasks that use
            // devMavenRepositories as an input will never be up-to-date.
            // The Gradle Module Metadata contains the same information (and more),
            // so the Maven metadata is redundant.
            exclude("**/maven-metadata*.xml")
        }

    /**
     * Configures [task] to register [devMavenRepositories] as a task input,
     * and (if possible) adds `devMavenRepository` as a [JavaForkOptions.systemProperty].
     */
    fun configureTask(task: Task) {
        task.inputs.files(devMavenRepositoriesInputFiles)
            .withPropertyName("devMavenPublish.devMavenRepositoriesInputFiles")
            .withPathSensitivity(RELATIVE)

        task.dependsOn(devMavenRepositories)

        if (task is JavaForkOptions) {
            task.doFirst("devMavenRepositories systemProperty") {
                // workaround https://github.com/gradle/gradle/issues/24267
                task.systemProperty(
                    "devMavenRepositories",
                    devMavenRepositories.joinToString(",") { it.canonicalFile.invariantSeparatorsPath }
                )
            }
        }
    }

    companion object {
        const val DEV_MAVEN_PUBLISH_EXTENSION_NAME = "devMavenPublish"
    }
}
