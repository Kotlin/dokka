/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.nio.file.Path
import kotlin.io.path.*

fun copyAndApplyGitDiff(
    projectDir: Path,
    diffFile: Path,
) {

    val diffFileText = diffFile.readText()

    projectDir.parent.resolve(diffFile.fileName).apply {
        createFile()
        writeText(diffFileText)
    }

    val projectGitFile = projectDir.resolve(".git")
    val git = if (projectGitFile.exists()) {
        if (projectGitFile.isRegularFile()) {
            println(".git file inside project directory exists, removing")
            removeGitFile(projectDir)
            Git.init().setDirectory(projectDir.toFile()).call()
        } else {
            println(".git directory inside project directory exists, reusing")
            FileRepositoryBuilder().apply {
                isMustExist = true
                gitDir = projectGitFile.toFile()
            }.let { Git(it.build()) }
        }
    } else {
        Git.init().setDirectory(projectDir.toFile()).call()
    }
    git.apply().setPatch(diffFile.inputStream()).call()
}

private fun removeGitFile(repository: Path) =
    repository.toFile()
        .listFiles().orEmpty()
        .filter { it.name.equals(".git", ignoreCase = true) }
        .forEach { it.delete() }
