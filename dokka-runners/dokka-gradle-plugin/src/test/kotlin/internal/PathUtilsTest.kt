/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.internal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.dokka.gradle.utils.tempDir
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createSymbolicLinkPointingTo

class PathUtilsTest : FunSpec({

    context("describe type") {
        val tempDir = tempDir()

        val regularFile = tempDir.resolve("regular-file.txt").createFile()
        val directory = tempDir.resolve("directory").createDirectories()

        val symlinkFile = tempDir.resolve("symlink2file").createSymbolicLinkPointingTo(regularFile)
        val symlinkDir = tempDir.resolve("symlink2dir").createSymbolicLinkPointingTo(directory)

        val nonExistent = tempDir.resolve("nonExistent")

        mapOf(
            regularFile to "file",
            directory to "directory",

            symlinkFile to "file (symbolic link)",
            symlinkDir to "directory (symbolic link)",

            nonExistent to "<non-existent>",
        ).forEach { (path, expected) ->
            test(expected) {
                path.describeType() shouldBe expected
            }
        }
    }
})
