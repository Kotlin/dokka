package dokkabuild.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

@CacheableTask
abstract class GenerateDokkaGradlePluginConstants @Inject constructor(
    private val fs: FileSystemOperations
) : DefaultTask() {

    @get:OutputDirectory
    abstract val destinationDir: DirectoryProperty

    @get:Input
    abstract val properties: MapProperty<String, String>

    init {
        group = project.name
    }

    @TaskAction
    fun action() {
        val properties = properties.get()

        // prepare temp dir
        fs.delete { delete(temporaryDir) }

        // generate file
        val vals = properties.entries
            .sortedBy { it.key }
            .joinToString("\n") { (k, v) ->
                """const val $k = "$v""""
            }.prependIndent("  ")

        temporaryDir.resolve("DokkaConstants.kt").apply {
            parentFile.mkdirs()
            writeText(
                """
                |/*
                | * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
                | */
                |package org.jetbrains.dokka.gradle.internal
                |
                |@DokkaInternalApi
                |object DokkaConstants {
                |$vals
                |}
                |
                """.trimMargin()
            )
        }

        // sync file to output dir
        fs.sync {
            from(temporaryDir) {
                into("org/jetbrains/dokka/gradle/internal/")
            }
            into(destinationDir)
        }
    }
}
