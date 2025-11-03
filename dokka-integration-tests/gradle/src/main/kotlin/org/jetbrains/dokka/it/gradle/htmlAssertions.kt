/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import io.kotest.assertions.withClue
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.sequences.shouldNotBeEmpty
import io.kotest.matchers.string.shouldNotContainIgnoringCase
import java.nio.file.Path
import kotlin.io.path.*

fun assertNoUnknownClassErrorsInHtml(dokkaOutputDir: Path) {
    withClue("expect no 'unknown class' message in output files") {
        val htmlFiles = dokkaOutputDir.walk()
            .filter { it.isRegularFile() && it.extension == "html" }

        htmlFiles.shouldNotBeEmpty()

        htmlFiles.forEach { file ->
            val relativePath = file.relativeTo(dokkaOutputDir)
            withClue("$relativePath should not contain 'Error class: unknown class'/'ERROR CLASS'/'Error type'") {
                file.useLines { lines ->
                    lines.shouldForAll { line ->
                        line.shouldNotContainIgnoringCase("Error class")
                        line.shouldNotContainIgnoringCase("Error type")
                    }
                }
            }
        }
    }
}
