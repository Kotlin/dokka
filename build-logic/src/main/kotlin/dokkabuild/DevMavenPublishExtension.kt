/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package dokkabuild

import dokkabuild.utils.systemProperty
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
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
    private val devMavenRepositoriesInputFiles: Provider<Set<FileSystemLocation>> =
        devMavenRepositories
            .elements
            .map { files ->
                files
                    .filterNot {
                        // Exclude Maven Metadata files because they contain timestamps, meaning tasks that use
                        // devMavenRepositories as an input will never be up-to-date.
                        // The Gradle Module Metadata contains the same information (and more),
                        // so the Maven metadata is redundant.
                        it.asFile.name.run { startsWith("maven-metadata") && endsWith(".xml") }
                    }
                    // sort the elements to ensure that the contents are reproducible
                    .toSortedSet { a, b -> a.asFile.compareTo(b.asFile) }
            }

    /**
     * Configures [task] to register [devMavenRepositories] as a task input,
     * and (if possible) adds `devMavenRepositories` as a [JavaForkOptions.systemProperty].
     */
    fun configureTask(task: Task) {
        task.inputs.files(devMavenRepositoriesInputFiles)
            .withPropertyName("devMavenPublish.devMavenRepositoriesInputFiles")
            .withPathSensitivity(RELATIVE)
            .ignoreEmptyDirectories()

        task.dependsOn(devMavenRepositories)

        if (task is JavaForkOptions) {
            task.jvmArgumentProviders.systemProperty(
                "devMavenRepositories",
                devMavenRepositoriesInputFiles.map { paths ->
                    paths.joinToString(",") { it.asFile.invariantSeparatorsPath }
                },
            )
        }
    }

    companion object {
        const val DEV_MAVEN_PUBLISH_EXTENSION_NAME = "devMavenPublish"
    }
}
