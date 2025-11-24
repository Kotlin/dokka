/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.adapters

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.setProperty
import org.jetbrains.dokka.gradle.DokkaBasePlugin
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import org.jetbrains.dokka.gradle.internal.PluginIds
import java.io.File
import javax.inject.Inject

/**
 * Discovers Android Gradle Plugin specific configuration and uses it to configure Dokka.
 *
 * This is an internal Dokka plugin and should not be used externally.
 * It is not a standalone plugin, it requires [DokkaBasePlugin] is also applied.
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
            PluginIds.android.forEach { pluginId ->
                project.pluginManager.withPlugin(pluginId) {
                    project.pluginManager.apply(AndroidAdapter::class)
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
    try {
        val androidComponents = project.findAndroidComponentsExtension()
        if (androidComponents != null) {
            return AndroidExtensionWrapper.forAndroidComponents(
                androidComponents = androidComponents,
                providers = project.providers,
                objects = project.objects,
            )
        }
    } catch (ex: Exception) {
        logger.warn("AndroidExtensionWrapper: could not find AndroidComponentsExtension", ex)
    }

    return null
}


/**
 * Wrap the Android extension so that Dokka can still access the configuration names without
 * caring about the AGP version in use.
 */
private interface AndroidExtensionWrapper {

    fun variantsCompileClasspath(): FileCollection

    /**
     * Get the `android.jar` for the current project.
     */
    fun bootClasspath(): Provider<List<File>>

    companion object {

        fun forAndroidComponents(
            androidComponents: AndroidComponentsExtension<*, *, *>,
            providers: ProviderFactory,
            objects: ObjectFactory,
        ): AndroidExtensionWrapper {
            return object : AndroidExtensionWrapper {
                private val androidVariants: SetProperty<AndroidVariantInfo> =
                    objects.setProperty(AndroidVariantInfo::class)
                        .collectFrom(androidComponents)

                /**
                 * Need to double-wrap with [Provider] because AGP will only
                 * compute the boot classpath after the compilation options have been finalized.
                 * Otherwise, AGP throws `IllegalStateException: targetCompatibility is not yet finalized`.
                 * https://issuetracker.google.com/issues/461382865
                 */
                private val bootClasspath: Provider<Provider<List<RegularFile>>> =
                    providers.provider {
                        androidComponents
                            .sdkComponents
                            .bootClasspath
                    }

                /** Fetch all compilation-classpath files used by all variants. */
                override fun variantsCompileClasspath(): FileCollection {
                    val collector = objects.fileCollection()
                    androidVariants.get().forEach { variant ->
                        collector.from(variant.compileClasspath)
                    }
                    return collector
                }

                override fun bootClasspath(): Provider<List<File>> {
                    return bootClasspath.flatMap { bootClasspath ->
                        bootClasspath.map { contents ->
                            contents.map(RegularFile::getAsFile)
                        }
                    }
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
