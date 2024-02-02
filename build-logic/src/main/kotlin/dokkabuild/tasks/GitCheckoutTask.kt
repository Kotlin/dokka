/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package dokkabuild.tasks

import org.eclipse.jgit.api.Git
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Clones a remote Git repository into [GitCheckoutTask.destination].
 */
@CacheableTask
abstract class GitCheckoutTask @Inject constructor(
    private val fs: FileSystemOperations
) : DefaultTask() {

    @get:OutputDirectory
    abstract val destination: DirectoryProperty

    /** Public HTTP URI of the Git repo. */
    @get:Input
    abstract val uri: Property<String>

    /** Specific git hash to checkout. */
    @get:Input
    abstract val commitId: Property<String>

    init {
        group = "git checkout"
    }

    @TaskAction
    fun action() {
        val uri = uri.get()
        val commitId = commitId.get()

        fs.delete { delete(temporaryDir) }

        Git.cloneRepository()
            .setURI(uri)
            .setDepth(1) // only checkout a single commit, to aid speed and Gradle caching
            .setDirectory(temporaryDir)
            .call().use { git ->
                git.pull().call()

                git.checkout()
                    .setName(commitId)
                    .call()

                fs.sync {
                    from(temporaryDir)
                    into(destination)
                    // exclude the .git dir to prevent the root git repo getting confused with a nested git repo
                    exclude(".git/")
                }

                logger.lifecycle("initialized git repo $uri in ${destination.asFile.orNull}")
            }
    }
}
