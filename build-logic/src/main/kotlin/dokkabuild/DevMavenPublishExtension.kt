/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package dokkabuild

import dokkabuild.utils.systemProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.testing.Test
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
    private val devMavenRepositoriesInputFiles: FileCollection =
        devMavenRepositories
            // Convert to a FileTree, which converts directories to all files, so we can filter on specific files.
            .asFileTree
            // Exclude Maven Metadata files because they contain timestamps, meaning tasks that use
            // devMavenRepositories as an input will never be up-to-date.
            // The Gradle Module Metadata contains the same information (and more),
            // so the Maven metadata is redundant.
            .matching { exclude("**/maven-metadata*.xml") }

    /**
     * Configures [Test] task to register [devMavenRepositories] as a task input,
     * and (if possible) adds `devMavenRepositories` as a [JavaForkOptions.systemProperty].
     */
    fun configureTask(task: Test) {
        task.inputs.files(devMavenRepositoriesInputFiles)
            .withPropertyName("devMavenPublish.devMavenRepositoriesInputFiles")
            .withPathSensitivity(RELATIVE)

        task.dependsOn(devMavenRepositories)
        task.systemProperty.internalProperty(
            "devMavenRepositories",
            devMavenRepositories.elements.map { paths ->
                paths.joinToString(",") { it.asFile.canonicalFile.invariantSeparatorsPath }
            }
        )
    }

    companion object {
        const val DEV_MAVEN_PUBLISH_EXTENSION_NAME = "devMavenPublish"
    }
}
