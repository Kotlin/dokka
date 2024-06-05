/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package dokkabuild.utils

import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskInputFilePropertyBuilder
import org.gradle.api.tasks.TaskInputPropertyBuilder
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.process.CommandLineArgumentProvider
import java.io.File
import javax.inject.Inject


/**
 * Utility for adding a System Property command line arguments to this [Test] task,
 * and correctly registering the values as task inputs (for Gradle up-to-date checks).
 */
// https://github.com/gradle/gradle/issues/11534
// https://github.com/gradle/gradle/issues/12247
val Test.systemProperty: SystemPropertyAdder
    get() {
        val spa = extensions.findByType<SystemPropertyAdder>()
            ?: extensions.create<SystemPropertyAdder>("SystemPropertyAdder", this)
        return spa
    }


abstract class SystemPropertyAdder @Inject internal constructor(
    private val task: Test,
) {
    fun inputDirectory(
        key: String,
        value: Directory,
    ): TaskInputFilePropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, value) {
                it.asFile.invariantSeparatorsPath
            }
        )
        return task.inputs.dir(value)
            .withPropertyName("SystemProperty input directory $key")
    }

    fun inputFile(
        key: String,
        file: RegularFile,
    ): TaskInputFilePropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, file) {
                it.asFile.invariantSeparatorsPath
            }
        )
        return task.inputs.file(file)
            .withPropertyName("SystemProperty input file $key")
    }

    fun inputFile(
        key: String,
        file: Provider<out RegularFile>,
    ): TaskInputFilePropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, file) {
                it.orNull?.asFile?.invariantSeparatorsPath
            }
        )
        return task.inputs.file(file)
            .withPropertyName("SystemProperty input file $key")
    }

    fun inputFiles(
        key: String,
        files: Provider<out FileCollection>,
    ): TaskInputFilePropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, files) { it.orNull?.asPath }
        )
        return task.inputs.files(files)
            .withPropertyName("SystemProperty input files $key")
    }

    fun inputProperty(
        key: String,
        value: Provider<out String>,
    ): TaskInputPropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, value) { it.orNull }
        )
        return task.inputs.property("SystemProperty input property $key", value)
    }

    @JvmName("inputBooleanProperty")
    fun inputProperty(
        key: String,
        value: Provider<out Boolean>,
    ): TaskInputPropertyBuilder = inputProperty(key, value.map { it.toString() })
}

private class SystemPropertyArgumentProvider<T : Any>(
    @get:Input
    val key: String,
    private val value: T,
    private val transformer: (value: T) -> String?,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> {
        val value = transformer(value) ?: return emptyList()
        return listOf("-D$key=$value")
    }
}

/**
 * Add a System Property (in the format `-D$key=$value`).
 *
 * [value] will _not_ be registered as a Gradle [org.gradle.api.Task] input.
 * (Which might be beneficial in cases where the property is optional, or not reproducible,
 * as such a property might disrupt task caching.)
 *
 * If you want to register the property as a Task input, use the
 * [Test.systemProperty][dokkabuild.utils.systemProperty] above instead.
 */
fun MutableList<CommandLineArgumentProvider>.systemProperty(
    key: String,
    value: Provider<String>,
) {
    add(
        SystemPropertyArgumentProvider(
            key = key,
            value = value,
            transformer = { it.orNull },
        )
    )
}
