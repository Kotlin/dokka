/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it

import java.util.concurrent.TimeUnit

class ProcessResult(
    val exitCode: Int,
    val output: String
)

fun Process.awaitProcessResult(): ProcessResult {
    waitFor(60, TimeUnit.SECONDS)

    val output = inputStream.reader().readText()
    println(output)

    return ProcessResult(
        exitCode = exitValue(),
        output = output,
    )
}
