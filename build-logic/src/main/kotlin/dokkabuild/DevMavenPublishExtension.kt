/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package dokkabuild

import dokkabuild.utils.systemProperty
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.process.JavaForkOptions
import java.io.File

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
    private val devMavenRepositoriesInputFiles: Provider<List<File>> =
        devMavenRepositories
            // Convert to a FileTree, which converts directories to all files, so we can filter on specific files.
            .asFileTree
            // Exclude Maven Metadata files because they contain timestamps, meaning tasks that use
            // devMavenRepositories as an input will never be up-to-date.
            // The Gradle Module Metadata contains the same information (and more),
            // so the Maven metadata is redundant.
            .matching { exclude("**/maven-metadata*.xml") }
            // FileTrees have an unstable order (even on the same machine), which means Gradle up-to-date checks fail.
            // So, manually sort the files so that Gradle can cache the task.
            .elements
            .map { files -> files.map { it.asFile }.sorted() }

    /**
     * Configures [task] to register [devMavenRepositories] as a task input,
     * and (if possible) adds `devMavenRepositories` as a [JavaForkOptions.systemProperty].
     */
    fun configureTask(task: Task) {
        task.inputs.files(devMavenRepositoriesInputFiles)
            .withPropertyName("devMavenPublish.devMavenRepositoriesInputFiles")
            .withPathSensitivity(RELATIVE)

        task.dependsOn(devMavenRepositories)

        if (task is JavaForkOptions) {
            task.jvmArgumentProviders.systemProperty(
                "devMavenRepositories",
                devMavenRepositories.elements.map { paths ->
                    paths.joinToString(",") { it.asFile.canonicalFile.invariantSeparatorsPath }
                }
            )
        }
    }

    companion object {
        const val DEV_MAVEN_PUBLISH_EXTENSION_NAME = "devMavenPublish"
    }
}
