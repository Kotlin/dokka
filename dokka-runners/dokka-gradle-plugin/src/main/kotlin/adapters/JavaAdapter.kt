/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.adapters

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import org.jetbrains.dokka.gradle.internal.PluginId
import org.jetbrains.dokka.gradle.internal.or
import org.jetbrains.dokka.gradle.internal.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import javax.inject.Inject

/**
 * Discovers Java Gradle Plugin specific configuration and uses it to configure Dokka.
 *
 * This is an internal Dokka plugin and should not be used externally.
 * It is not a standalone plugin, it requires [org.jetbrains.dokka.gradle.DokkaBasePlugin] is also applied.
 */
@InternalDokkaGradlePluginApi
abstract class JavaAdapter @Inject constructor(
    private val providers: ProviderFactory,
) : Plugin<Project> {

    private val logger = Logging.getLogger(this::class.java)

    override fun apply(project: Project) {
        logger.info("applied DokkaJavaAdapter to ${project.path}")

        val dokkaExtension = project.extensions.getByType<DokkaExtension>()

        val java = project.extensions.getByType<JavaPluginExtension>()
        val sourceSets = project.extensions.getByType<SourceSetContainer>()

        detectJavaToolchainVersion(dokkaExtension, java)

        val isConflictingPluginPresent = isConflictingPluginPresent(project)
        registerDokkaSourceSets(dokkaExtension, sourceSets, isConflictingPluginPresent)
    }

    /** Fetch the  toolchain, and use the language version as Dokka's jdkVersion */
    private fun detectJavaToolchainVersion(
        dokkaExtension: DokkaExtension,
        java: JavaPluginExtension,
    ) {
        // fetch the toolchain, and use the language version as Dokka's jdkVersion
        val toolchainLanguageVersion = java
            .toolchain
            .languageVersion

        dokkaExtension.dokkaSourceSets.configureEach {
            jdkVersion.set(toolchainLanguageVersion.map { it.asInt() }.orElse(11))
        }
    }

    private fun registerDokkaSourceSets(
        dokkaExtension: DokkaExtension,
        sourceSets: SourceSetContainer,
        isConflictingPluginPresent: Provider<Boolean>,
    ) {
        sourceSets.all jss@{
            register(
                dokkaSourceSets = dokkaExtension.dokkaSourceSets,
                src = this@jss,
                isConflictingPluginPresent = isConflictingPluginPresent,
            )
        }
    }

    /** Register a single [DokkaSourceSetSpec] for [src] */
    private fun register(
        dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetSpec>,
        src: SourceSet,
        isConflictingPluginPresent: Provider<Boolean>,
    ) {
        dokkaSourceSets.register(
            "java${src.name.uppercaseFirstChar()}"
        ) {
            suppress.convention(!src.isPublished() or isConflictingPluginPresent)
            sourceRoots.from(src.java)
            analysisPlatform.convention(KotlinPlatform.JVM)

            classpath.from(providers.provider { src.compileClasspath })
            classpath.builtBy(src.compileJavaTaskName)
        }
    }

    /**
     * The Android and Kotlin plugins _also_ add the Java plugin.
     *
     * To prevent generating documentation for the same sources twice, automatically suppress any
     * [DokkaSourceSetSpec] when any Android or Kotlin plugin is present
     *
     * Projects with Android or Kotlin projects present will be handled by [AndroidAdapter]
     * or [KotlinAdapter].
     */
    private fun isConflictingPluginPresent(
        project: Project
    ): Provider<Boolean> {

        val projectHasKotlinPlugin = providers.provider {
            PluginId.kgpPlugins.any { project.pluginManager.hasPlugin(it) }
        }

        val projectHasAndroidPlugin = providers.provider {
            PluginId.androidPlugins.any { project.pluginManager.hasPlugin(it) }
        }

        return projectHasKotlinPlugin or projectHasAndroidPlugin
    }

    @InternalDokkaGradlePluginApi
    companion object {
        /**
         * Determine if a [KotlinCompilation] is 'publishable', and so should be enabled by default
         * when creating a Dokka publication.
         *
         * Typically, 'main' compilations are publishable and 'test' compilations should be suppressed.
         * This can be overridden manually, though.
         *
         * @see DokkaSourceSetSpec.suppress
         */
        fun SourceSet.isPublished(): Boolean =
            name != TEST_SOURCE_SET_NAME
                    && name.startsWith(MAIN_SOURCE_SET_NAME)

        internal fun applyTo(project: Project) {
            project.plugins.withType<JavaBasePlugin>().all {
                project.pluginManager.apply(type = JavaAdapter::class)
            }
        }
    }
}
