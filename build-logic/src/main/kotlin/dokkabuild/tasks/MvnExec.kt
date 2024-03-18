/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package dokkabuild.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitivity.NONE
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.process.ExecOperations
import org.gradle.work.NormalizeLineEndings
import java.util.*
import javax.inject.Inject

/**
 * Runs a Maven task.
 *
 * See `dokkabuild.setup-maven-cli.gradle.kts` for details on the Maven CLI installation.
 */
@CacheableTask
abstract class MvnExec
@Inject
constructor(
    private val exec: ExecOperations,
    private val fs: FileSystemOperations,
) : DefaultTask() {

    /**
     * Work directory.
     *
     * Be aware that any existing content will be replaced by [filteredClasses] and [resources].
     */
    @get:OutputDirectory
    abstract val workDirectory: DirectoryProperty

    /** Input classes - will be synced to [workDirectory]. */
    @get:Internal
    abstract val classes: ConfigurableFileCollection

    @get:Classpath
    protected val filteredClasses: FileCollection
        get() = classes.asFileTree.matching { include("**/*.class") }

    /** Input resource files - will be synced to [workDirectory]. */
    @get:InputFiles
    @get:PathSensitive(RELATIVE)
    @get:NormalizeLineEndings
    abstract val resources: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(NONE)
    abstract val mvnCli: RegularFileProperty

    @get:Input
    abstract val arguments: ListProperty<String>

    /** `-e` - Produce execution error messages. */
    @get:Input
    @get:Optional
    abstract val showErrors: Property<Boolean>

    /** `-B` - Run in non-interactive (batch) mode. */
    @get:Input
    @get:Optional
    abstract val batchMode: Property<Boolean>

    @TaskAction
    fun exec() {
        fs.sync {
            from(filteredClasses) {
                into("classes/java/main")
            }
            from(resources)
            into(workDirectory)
        }

        val arguments = buildList {
            addAll(arguments.get())
            if (showErrors.orNull == true) add("--errors")
            if (batchMode.orNull == true) add("--batch-mode")
        }

        exec.exec {
            workingDir(workDirectory)
            executable(mvnCli.get())
            args(arguments)
        }

        makePropertiesFilesReproducible()
    }

    /**
     * Remove non-reproducible timestamps from any generated [Properties] files.
     */
    private fun makePropertiesFilesReproducible() {
        workDirectory.get().asFile.walk()
            .filter { it.isFile && it.extension == "properties" }
            .forEach { file ->
                logger.info("[MvnExec $path] removing timestamp from $file")
                // drop the last comment - java.util.Properties always adds a timestamp, which is not-reproducible.
                val comments = file.readLines()
                    .takeWhile { it.startsWith('#') }
                    .dropLast(1)

                val properties = file.readLines().dropWhile { it.startsWith('#') }

                val updatedProperties = (comments + properties).joinToString("\n")
                file.writeText(updatedProperties)
            }
    }
}
