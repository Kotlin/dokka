/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.internal

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink

/**
 * Returns the type of file (file, directory, symlink, etc.).
 */
internal fun Path.describeType(): String =
    buildString {
        append(
            when {
                !exists() -> "<non-existent>"
                isRegularFile() -> "file"
                isDirectory() -> "directory"
                else -> "<unknown>"
            }
        )

        if (isSymbolicLink()) {
            append(" (symbolic link)")
        }
    }
