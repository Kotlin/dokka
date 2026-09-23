/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation.pages

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File

/**
 * Prints the top-level keys of a KDM json.
 *
 * Run it from the IDE with the gutter icon next to [main] — with no arguments it reads
 * [DEFAULT_MODEL] from the reference output. Pass a path to read another file.
 */
private const val DEFAULT_MODEL =
    "dokka-integration-tests/gradle/build/ui-showcase-result/jvm/kdm/model.json"

fun main(args: Array<String>) {
    val file = args.firstOrNull()?.let(::File) ?: File(repoRoot(), DEFAULT_MODEL)
    println(file.absolutePath)
    if (!file.isFile) {
        println("  no such file")
        return
    }
    Json.parseToJsonElement(file.readText()).jsonObject.keys.forEach { println("  $it") }
}

private fun repoRoot(): File = generateSequence(File("").absoluteFile) { it.parentFile }
    .firstOrNull { File(it, "settings.gradle.kts").isFile }
    ?: File("").absoluteFile
