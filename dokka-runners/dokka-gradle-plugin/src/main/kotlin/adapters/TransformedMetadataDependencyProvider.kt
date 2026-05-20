/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.adapters

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.dokka.gradle.internal.debug
import org.jetbrains.dokka.gradle.internal.locateOrRegisterMetadataDependencyTransformationTaskCompat
import org.jetbrains.dokka.gradle.internal.logWarningWithStacktraceHint
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyTransformationTask
import java.io.File

/**
 * Workaround for KT-80551.
 *
 * KGP creates metadata compilations for shared source sets with multiple native compilations,
 * but _not_ for source sets with multiple Wasm targets.
 * So, if a project has a shared 'Wasm' source set for WasmJS and WasmWASI,
 * KGP will not create a metadata compilation.
 * This means there's no `.klib` for kotlin-stdlib, so Dokka can't see shared symbols
 * (like [kotlin.AutoCloseable]).
 *
 * This will eventually be fixed in KGP.
 * Until then, DGP can work around the issue by using KGP internals to register the required metadata transformations.
 *
 * This workaround can be disabled,
 * see [org.jetbrains.dokka.gradle.internal.PluginFeaturesService.enableWorkaroundKT80551].
 */
internal class TransformedMetadataDependencyProvider(private val project: Project) {
    fun get(kss: KotlinSourceSet): FileCollection {
        val files = project.objects.fileCollection()

        val transformationTask =
            try {
                project.locateOrRegisterMetadataDependencyTransformationTaskCompat(kss)
            } catch (e: Throwable) {
                // Don't re-throw exceptions because we don't want to fail project sync.
                project.logWarningWithStacktraceHint(e) {
                    "Failed to access MetadataDependencyTransformationTask for source set ${kss.name}"
                }
                null
            }
        if (transformationTask != null) {
            logger.info("registered MetadataDependencyTransformationTask ${transformationTask.name} for ${kss.name}")
            files.from(
                transformationTask.allTransformedLibraries()
            )
        }
        return files
    }

    private fun TaskProvider<MetadataDependencyTransformationTask>.allTransformedLibraries(): Provider<Provider<List<File>>> {
        return map { task ->
            val taskPath = task.path // redefined, for configuration-cache compatibility

            val allTransformedLibraries =
                try {
                    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
                    task.allTransformedLibraries()
                } catch (e: Throwable) {
                    // Don't re-throw exceptions because we don't want to fail project sync.
                    project.logWarningWithStacktraceHint(e) {
                        "Failed to access allTransformedLibraries from task ${task.path}"
                    }
                    null
                }

            allTransformedLibraries?.map { libs ->
                logger.debug { "$taskPath allTransformedLibraries ${libs.map { it.name }}" }
                libs
            }
        }
    }

    companion object {
        private val logger: Logger = Logging.getLogger(TransformedMetadataDependencyProvider::class.java)
    }
}
