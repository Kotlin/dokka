/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

public class ProcessResult(
    public val exitCode: Int,
    public val output: String
)

public fun Process.awaitProcessResult(): ProcessResult = runBlocking {
    val exitCode = async { awaitExitCode() }
    val output = async { awaitOutput() }
    ProcessResult(
        exitCode.await(),
        output.await()
    )
}

private suspend fun Process.awaitExitCode(): Int {
    val deferred = CompletableDeferred<Int>()
    thread {
        try {
            deferred.complete(this.waitFor())
        } catch (e: Throwable) {
            deferred.completeExceptionally(e)
        }
    }

    return deferred.await()
}

private suspend fun Process.awaitOutput(): String {
    val deferred = CompletableDeferred<String>()
    thread {
        try {
            var string = ""
            this.inputStream.bufferedReader().forEachLine { line ->
                println(line)
                string += line + System.lineSeparator()
            }
            deferred.complete(string)
        } catch (e: Throwable) {
            deferred.completeExceptionally(e)
        }
    }

    return deferred.await()
}
