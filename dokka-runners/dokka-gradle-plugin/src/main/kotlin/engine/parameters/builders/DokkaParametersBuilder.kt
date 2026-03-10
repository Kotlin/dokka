/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.parameters.builders

import kotlinx.serialization.json.JsonObject
import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.jetbrains.dokka.*
import org.jetbrains.dokka.gradle.DokkaBasePlugin
import org.jetbrains.dokka.gradle.engine.parameters.DokkaGeneratorParametersSpec
import org.jetbrains.dokka.gradle.engine.parameters.DokkaModuleDescriptionKxs
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.engine.plugins.DokkaPluginParametersBaseSpec
import org.jetbrains.dokka.gradle.formats.DokkaHtmlPlugin.Companion.extractDokkaPluginMarkers
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import java.io.File

/**
 * Convert the Gradle-focused [DokkaGeneratorParametersSpec] into a [DokkaSourceSetImpl] instance,
 * which will be passed to Dokka Generator.
 *
 * The conversion is defined in a separate class to try and prevent classes from Dokka Generator
 * leaking into the public API.
 */
@InternalDokkaGradlePluginApi
internal class DokkaParametersBuilder(
    private val archives: ArchiveOperations,
) {

    private val logger: Logger = Logging.getLogger(DokkaParametersBuilder::class.java)

    fun build(
        spec: DokkaGeneratorParametersSpec,
        delayTemplateSubstitution: Boolean,
        outputDirectory: File,
        cacheDirectory: File? = null,
        moduleDescriptorDirs: Iterable<File>,
    ): DokkaConfiguration {
        verifySourceRootsIntersectionsBasedOnAndroidVariants(spec.dokkaSourceSets)

        val moduleName = spec.moduleName.get()
        val moduleVersion = spec.moduleVersion.orNull?.takeIf { it != Project.DEFAULT_VERSION }
        val offlineMode = spec.offlineMode.get()
        val sourceSets = DokkaSourceSetBuilder.buildAll(spec.dokkaSourceSets)
        val failOnWarning = spec.failOnWarning.get()
        val suppressObviousFunctions = spec.suppressObviousFunctions.get()
        val suppressInheritedMembers = spec.suppressInheritedMembers.get()
        val finalizeCoroutines = spec.finalizeCoroutines.get()
        val pluginsConfiguration = spec.pluginsConfiguration.toSet()

        val pluginsClasspath = buildPluginsClasspath(
            plugins = spec.pluginsClasspath,
        )

        val includes = spec.includes.files

        return DokkaConfigurationImpl(
            moduleName = moduleName,
            moduleVersion = moduleVersion,
            outputDir = outputDirectory,
            cacheRoot = cacheDirectory,
            offlineMode = offlineMode,
            sourceSets = sourceSets,
            pluginsClasspath = pluginsClasspath,
            pluginsConfiguration = pluginsConfiguration.map(::build),
            modules = buildModuleDescriptors(moduleDescriptorDirs),
            failOnWarning = failOnWarning,
            delayTemplateSubstitution = delayTemplateSubstitution,
            suppressObviousFunctions = suppressObviousFunctions,
            includes = includes,
            suppressInheritedMembers = suppressInheritedMembers,
            finalizeCoroutines = finalizeCoroutines,
        )
    }

    private fun buildPluginsClasspath(
        plugins: FileCollection,
    ): List<File> {
        // only include dependencies with Dokka Plugin markers
        return plugins
            .filter { file ->
                val pluginIds = extractDokkaPluginMarkers(archives, file)
                pluginIds.isNotEmpty()
            }
            .toList()
    }

    private fun buildModuleDescriptors(
        moduleDescriptorDirs: Iterable<File>
    ): List<DokkaModuleDescriptionImpl> {
        return moduleDescriptorDirs
            .map { moduleDir ->
                val moduleDescriptorJson = moduleDir.resolve("module-descriptor.json")
                if (!moduleDir.exists()) {
                    error("missing module-descriptor.json in consolidated Dokka module $moduleDir")
                }

                val moduleDescriptor: DokkaModuleDescriptionKxs =
                    DokkaModuleDescriptionKxs.fromJsonObject(
                        DokkaBasePlugin.jsonMapper.decodeFromString(
                            JsonObject.serializer(),
                            moduleDescriptorJson.readText(),
                        )
                    )

                val moduleOutputDirectory = moduleDir.resolve(moduleDescriptor.moduleOutputDirName)
                if (!moduleOutputDirectory.exists()) {
                    error("missing module output directory in consolidated Dokka module $moduleDir")
                }

                val moduleIncludes = moduleDir.resolve(moduleDescriptor.moduleIncludesDirName)
                    .takeIf(File::exists)
                    ?.walk()
                    ?.drop(1)
                    ?.toSet()
                    ?: emptySet()  // 'include' files are optional


                // `relativeOutputDir` is the path where the Dokka Module should be located within the final
                // Dokka Publication.
                // Convert a project path to a relative path
                // e.g. `:x:y:z:my-cool-subproject` → `x/y/z/my-cool-subproject`.
                // The path has to be unique per module - using the project path is a useful way to achieve this.
                val relativeOutputDir =
                    File(moduleDescriptor.modulePath.removePrefix(":").replace(':', '/'))

                val md = DokkaModuleDescriptionImpl(
                    name = moduleDescriptor.name,
                    relativePathToOutputDirectory = relativeOutputDir,
                    includes = moduleIncludes,
                    sourceOutputDirectory = moduleOutputDirectory,
                )

                logger.info("[${this::class}] converted $moduleDir to $md")

                md
            }
            // Sort so the output is stable.
            // `relativePathToOutputDirectory` is better than `name` since it's guaranteed to be unique
            // across all modules (otherwise they'd be generated into the same directory), and even
            // though it's a file - it's a _relative_ file, so the ordering should be stable across
            // machines (which is important for relocatable Build Cache).
            .sortedBy { it.relativePathToOutputDirectory.invariantSeparatorsPath }
    }

    private fun build(spec: DokkaPluginParametersBaseSpec): PluginConfigurationImpl {
        return PluginConfigurationImpl(
            fqPluginName = spec.pluginFqn,
            serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
            values = spec.jsonEncode(),
        )
    }

    private fun verifySourceRootsIntersectionsBasedOnAndroidVariants(sourceSets: Set<DokkaSourceSetSpec>) {
        // no check in case there is only single source-set
        if (sourceSets.size <= 1) return

        // check only source sets based on Android variants;
        // everything else should be handled by the default checker in analysis
        val sourceRootsIntersections = detectSourceRootsIntersections(
            sourceSets.filter { it.basedOnAndroidVariant.getOrElse(false) }
        )

        if (sourceRootsIntersections.isEmpty()) return

        val intersectedSourceSets = mutableSetOf<String>().apply {
            sourceRootsIntersections.forEach { (s1Name, s2Name, _) ->
                add(s1Name)
                add(s2Name)
            }
        }

        error(
            """
            |Dokka cannot generate documentation for Android projects with multiple enabled variants that have common source roots.
            |Please suppress all variants except ONE via the following configuration:
            |dokka {
            |    dokkaSourceSets.configureEach {
            |        suppress.set(name != VARIANT_NAME)
            |    }
            |}
            |Where VARIANT_NAME could be one of the following: $intersectedSourceSets
            |For more information regarding the reasoning behind this restriction: https://github.com/Kotlin/dokka/issues/4472
            |Common source roots:
            |${
                sourceRootsIntersections.joinToString("\n") { (s1Name, s2Name, intersectedPaths) ->
                    "  '$s1Name' and '$s2Name' have common source roots:\n${
                        intersectedPaths.joinToString("\n") {
                            "    - ${it.absolutePath}"
                        }
                    }"
                }
            }""".trimMargin()
        )
    }

    private data class SourceRootIntersections(
        val s1Name: String,
        val s2Name: String,
        val intersectedPaths: Set<File>,
    )

    // based on SourceRootIndependentChecker
    private fun detectSourceRootsIntersections(sourceSets: List<DokkaSourceSetSpec>): List<SourceRootIntersections> {
        fun Set<File>.normalize() = mapTo(mutableSetOf(), File::normalize)
        fun intersectOfNormalizedPaths(normalizedPaths: Set<File>, normalizedPaths2: Set<File>): Set<File> {
            val result = mutableSetOf<File>()
            for (p1 in normalizedPaths) {
                for (p2 in normalizedPaths2) {
                    if (p1.startsWith(p2) || p2.startsWith(p1)) {
                        result.add(p1)
                        result.add(p2)
                    }
                }
            }
            return result
        }

        fun intersect(paths: Set<File>, paths2: Set<File>): Set<File> {
            return intersectOfNormalizedPaths(paths.normalize(), paths2.normalize())
        }

        // no check in case there is only single source-set
        if (sourceSets.size <= 1) return emptyList()

        val intersections = mutableListOf<SourceRootIntersections>()
        for (i in sourceSets.indices) {
            for (j in i + 1 until sourceSets.size) {
                val intersection = intersect(sourceSets[i].sourceRoots.files, sourceSets[j].sourceRoots.files)
                if (intersection.isNotEmpty()) {
                    intersections.add(
                        SourceRootIntersections(
                            s1Name = sourceSets[i].name,
                            s2Name = sourceSets[j].name,
                            intersectedPaths = intersection
                        )
                    )
                }
            }
        }
        return intersections
    }
}
