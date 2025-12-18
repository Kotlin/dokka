/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.dokka.gradle.adapters

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.logging.configuration.ShowStacktrace
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask
import java.io.File
import kotlin.jvm.java

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
        try {
            val transformationTask = project.locateOrRegisterMetadataDependencyTransformationTask(kss)
            logger.info("registered MetadataDependencyTransformationTask ${transformationTask.name} for ${kss.name}")
            files.from(
                transformationTask.allTransformedLibraries()
            )
        } catch (e: Throwable) {
            // Don't re-throw exceptions because we don't want to fail project sync.
            if (project.gradle.startParameter.showStacktrace == ShowStacktrace.ALWAYS) {
                logger.warn("Failed to access MetadataDependencyTransformationTask for source set ${kss.name}.", e)
            } else {
                logger.warn("Failed to access MetadataDependencyTransformationTask for source set ${kss.name}. Run with `--stacktrace` for more details.")
            }
        }

        return files
    }

    private fun TaskProvider<MetadataDependencyTransformationTask>.allTransformedLibraries(): Provider<Provider<List<File>>> {
        return map { task ->
            val taskPath = task.path
            task.allTransformedLibraries().map { libs ->
                logger.debug("$taskPath allTransformedLibraries ${libs.map { it.name }}")
                libs
            }
        }
    }

    companion object {
        private val logger: Logger = Logging.getLogger(TransformedMetadataDependencyProvider::class.java)
    }
}
