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

// TODO rename
@CacheableTask
abstract class GenerateDokkatooConstants @Inject constructor(
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

        temporaryDir.resolve("DokkatooConstants.kt").apply {
            parentFile.mkdirs()
            writeText(
                """
        |package org.jetbrains.dokka.gradle.internal
        |
        |@DokkatooInternalApi
        |object DokkatooConstants {
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
