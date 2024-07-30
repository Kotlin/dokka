/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package dokkabuild.tasks

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand.ResetType.HARD
import org.eclipse.jgit.lib.NullProgressMonitor
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.lib.TextProgressMonitor
import org.eclipse.jgit.util.FS
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logging
import org.gradle.api.provider.*
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.of
import java.io.File
import javax.inject.Inject

/**
 * Clones a remote Git repository into [GitCheckoutTask.destination].
 */
@CacheableTask
abstract class GitCheckoutTask @Inject constructor(
    private val fs: FileSystemOperations,
    private val providers: ProviderFactory,
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
        outputs.upToDateWhen { task ->
            require(task is GitCheckoutTask)
            task.gitRepoIsUpToDate().get()
        }
    }

    private val localRepoDir: File
        get() = temporaryDir.resolve("repo")

    private val gitOperationsPrinter: ProgressMonitor =
        if (logger.isInfoEnabled) {
            TextProgressMonitor()
        } else {
            NullProgressMonitor.INSTANCE
        }

    init {
        group = "git checkout"
    }

    @TaskAction
    fun action() {
        initializeRepo()

        fs.sync {
            from(localRepoDir)
            into(destination)
            // exclude the .git dir:
            // - prevent the root git repo getting confused with a nested git repo
            // - improves Gradle caching
            exclude(".git/")
        }

        logger.lifecycle("initialized git repo ${uri.get()} in ${destination.asFile.get()}")
    }

    /**
     * Initialize [uri] in [localRepoDir].
     *
     * If a git repo already exists in [localRepoDir], try to re-use it.
     *
     * Any changes to tracked or untracked files will be forcibly removed.
     */
    private fun initializeRepo() {
        val uri = uri.get()
        val commitId = commitId.get()

        // Check if the repo is already cloned. If yes, then we can re-use it to save time.
        val gitRepoInitialized = RepositoryCache.FileKey.isGitRepository(localRepoDir, FS.DETECTED)

        val repo = if (gitRepoInitialized) {
            // re-use existing cloned repo
            Git.open(localRepoDir)
        } else {
            // repo is either not cloned or is not recognizable, so delete it and make a fresh clone
            fs.delete { delete(localRepoDir) }

            Git.cloneRepository()
                .setProgressMonitor(gitOperationsPrinter)
                .setNoCheckout(true)
                .setURI(uri)
                .setDirectory(localRepoDir)
                .call()
        }

        repo.use { git ->
            // checkout the specific commitId specified in the task input
            git.checkout()
                .setProgressMonitor(gitOperationsPrinter)
                .setForced(true)
                .setName(commitId)
                .call()

            // git reset --hard (wipe changes to tracked files, if any)
            git.reset()
                .setProgressMonitor(gitOperationsPrinter)
                .setMode(HARD)
                .call()

            // git clean -fdx (remove any changes in untracked files)
            git.clean()
                .setForce(true)
                .setCleanDirectories(true)
                .setIgnore(false)
                .call()
        }
    }

    private fun gitRepoIsUpToDate(): Provider<Boolean> =
        providers.of(GitRepoIsUpToDate::class) {
            parameters.repoDir = localRepoDir
        }

    /**
     * Determine if [repoDir][GitRepoIsUpToDate.Params.repoDir] is a valid Git repo,
     * and it does not require cleaning.
     */
    internal abstract class GitRepoIsUpToDate : ValueSource<Boolean, GitRepoIsUpToDate.Params> {
        interface Params : ValueSourceParameters {
            val repoDir: DirectoryProperty
        }

        private val repoDir: File get() = parameters.repoDir.get().asFile

        private val logger = Logging.getLogger(GitRepoIsUpToDate::class.java)
        private fun log(msg: String): Unit = logger.info("[GitRepoIsUpToDate] $msg (repoDir=$repoDir)")


        override fun obtain(): Boolean {
            val gitRepoInitialized = RepositoryCache.FileKey.isGitRepository(repoDir, FS.DETECTED)

            if (!gitRepoInitialized) {
                logger.info("repo is either not cloned or is not recognizable as a git repo")
                return false
            }

            Git.open(repoDir).use { git ->
                val status = git.status().call()
                return when {
                    status.hasUncommittedChanges() -> {
                        log("repo has ${status.uncommittedChanges.size} uncommited changes")
                        false
                    }

                    status.hasUncommittedChanges() -> {
                        log("repo has ${status.untracked.size} untracked changes")
                        false
                    }

                    else -> {
                        log("repo is up-to-date")
                        true
                    }
                }
            }

        }
    }
}
