/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.adapters

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaBasePlugin
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import org.jetbrains.dokka.gradle.internal.PluginIds
import org.jetbrains.dokka.gradle.internal.artifactType
import java.io.File
import javax.inject.Inject

/**
 * Discovers Android Gradle Plugin specific configuration and uses it to configure Dokka.
 *
 * This is an internal Dokka plugin and should not be used externally.
 * It is not a standalone plugin, it requires [org.jetbrains.dokka.gradle.DokkaBasePlugin] is also applied.
 */
@InternalDokkaGradlePluginApi
abstract class AndroidAdapter @Inject constructor(
    private val objects: ObjectFactory,
) : Plugin<Project> {

    override fun apply(project: Project) {
        logger.info("applied ${this::class} to ${project.path}")

        val dokkaExtension = project.extensions.getByType<DokkaExtension>()

        val androidExt = AndroidExtensionWrapper(project) ?: return

        dokkaExtension.dokkaSourceSets.configureEach {

            classpath.from(
                androidExt.bootClasspath()
            )

            classpath.from(
                analysisPlatform.map { analysisPlatform ->
                    when (analysisPlatform) {
                        KotlinPlatform.AndroidJVM ->
                            AndroidClasspathCollector(
                                androidExt = androidExt,
                                objects = objects,
                            )

                        else ->
                            objects.fileCollection()
                    }
                }
            )
        }
    }

    @InternalDokkaGradlePluginApi
    companion object {

        /**
         * Apply [AndroidAdapter] a single time to [project], regardless of how many AGP plugins are applied.
         */
        internal fun applyTo(project: Project) {
            project.plugins.withType<DokkaBasePlugin>().all {
                PluginIds.android.forEach { pluginId ->
                    project.pluginManager.withPlugin(pluginId) {
                        project.pluginManager.apply(AndroidAdapter::class)
                    }
                }
            }
        }
    }
}


private val logger = Logging.getLogger(AndroidAdapter::class.java)


/** Create a [AndroidExtensionWrapper] */
private fun AndroidExtensionWrapper(
    project: Project
): AndroidExtensionWrapper? {
    val androidExt: BaseExtension = try {
        project.extensions.getByType()
    } catch (ex: Exception) {
        logger.warn("${AndroidExtensionWrapper::class} could not get Android Extension for project ${project.path}")
        return null
    }
    return AndroidExtensionWrapper.forBaseExtension(
        androidExt = androidExt,
        providers = project.providers,
        objects = project.objects
    )
}


/**
 * Wrap the Android extension so that Dokka can still access the configuration names without
 * caring about the AGP version in use.
 */
private interface AndroidExtensionWrapper {

    fun variantsCompileClasspath(): FileCollection

    fun bootClasspath(): Provider<List<File>>

    companion object {

        fun forBaseExtension(
            androidExt: BaseExtension,
            providers: ProviderFactory,
            objects: ObjectFactory,
        ): AndroidExtensionWrapper {
            return object : AndroidExtensionWrapper {

                override fun variantsCompileClasspath(): FileCollection {
                    val androidComponentsCompileClasspath = objects.fileCollection()

                    val variants = when (androidExt) {
                        is LibraryExtension -> androidExt.libraryVariants
                        is AppExtension -> androidExt.applicationVariants
                        is TestExtension -> androidExt.applicationVariants
                        else -> {
                            logger.warn("${AndroidExtensionWrapper::class} found unknown Android Extension $androidExt")
                            return objects.fileCollection()
                        }
                    }

                    fun Configuration.collect(artifactType: String) {
                        val artifactTypeFiles = incoming
                            .artifactView {
                                attributes {
                                    artifactType(artifactType)
                                }
                                lenient(true)
                            }
                            .artifacts
                            .resolvedArtifacts
                            .map { artifacts -> artifacts.map(ResolvedArtifactResult::getFile) }

                        androidComponentsCompileClasspath.from(artifactTypeFiles)
                    }

                    variants.all {
                        compileConfiguration.collect("jar")
                        //runtimeConfiguration.collect("jar")
                    }

                    return androidComponentsCompileClasspath
                }

                override fun bootClasspath(): Provider<List<File>> {
                    return providers.provider { androidExt.bootClasspath }
                }
            }
        }
    }
}


/**
 * A utility for determining the classpath of an Android compilation.
 *
 * It's important that this class is separate from [AndroidAdapter]. It must be separate
 * because it uses Android Gradle Plugin classes (like [BaseExtension]). Were it not separate, and
 * these classes were present in the function signatures of [AndroidAdapter], then when
 * Gradle tries to create a decorated instance of [AndroidAdapter] it will throw an
 * exception if the project does not have the Android Gradle Plugin applied, because the AGP
 * classes will be missing.
 */
private object AndroidClasspathCollector {

    operator fun invoke(
        androidExt: AndroidExtensionWrapper,
        objects: ObjectFactory,
    ): FileCollection {
        val compilationClasspath = objects.fileCollection()

        compilationClasspath.from({ androidExt.variantsCompileClasspath() })

        return compilationClasspath
    }
}
