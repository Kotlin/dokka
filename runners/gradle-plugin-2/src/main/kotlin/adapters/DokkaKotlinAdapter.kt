package org.jetbrains.dokka.gradle.adapters

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.LibraryVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gradle.DokkaPluginSettings
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import javax.inject.Inject

/**
 * Apply Kotlin specific configuration to the Dokka Plugin
 */
abstract class DokkaKotlinAdapter @Inject constructor(
    private val objects: ObjectFactory
) : Plugin<Project> {

    private val logger = Logging.getLogger(this::class.java)

    override fun apply(project: Project) {
        logger.lifecycle("applied DokkaKotlinAdapter to ${project.path}")

        project.pluginManager.apply {
//            withPlugin("kotlin") { asd(project) }
//            withPlugin("kotlin-dsl") { asd(project) }
//            withPlugin("embedded-kotlin") { asd(project) }
//            withPlugin("kotlin-android") { asd(project) }
//            withPlugin("kotlin-android-extensions") { asd(project) }
//            withPlugin("kotlin-kapt") { asd(project) }
//            withPlugin("kotlin-multiplatform") { asd(project) }
//            withPlugin("kotlin-native-cocoapods") { asd(project) }
//            withPlugin("kotlin-native-performance") { asd(project) }
//            withPlugin("kotlin-parcelize") { asd(project) }
//            withPlugin("kotlin-platform-android") { asd(project) }
//            withPlugin("kotlin-platform-common") { asd(project) }
//            withPlugin("kotlin-platform-js") { asd(project) }
//            withPlugin("kotlin-platform-jvm") { asd(project) }
//            withPlugin("kotlin-scripting") { asd(project) }
//            withPlugin("org.jetbrains.kotlin.android.extensions") { asd(project) }
            withPlugin("org.jetbrains.kotlin.android") { asd(project) }
            withPlugin("org.jetbrains.kotlin.js") { asd(project) }
            withPlugin("org.jetbrains.kotlin.jvm") { asd(project) }
//            withPlugin("org.jetbrains.kotlin.kapt") { asd(project) }
//            withPlugin("org.jetbrains.kotlin.multiplatform.pm20") { asd(project) }
            withPlugin("org.jetbrains.kotlin.multiplatform") { asd(project) }
//            withPlugin("org.jetbrains.kotlin.native.cocoapods") { asd(project) }
//            withPlugin("org.jetbrains.kotlin.native.performance") { asd(project) }
//            withPlugin("org.jetbrains.kotlin.platform.android") { asd(project) }
//            withPlugin("org.jetbrains.kotlin.platform.common") { asd(project) }
//            withPlugin("org.jetbrains.kotlin.platform.js") { asd(project) }
//            withPlugin("org.jetbrains.kotlin.platform.jvm") { asd(project) }
//            withPlugin("org.jetbrains.kotlin.plugin.parcelize") { asd(project) }
//            withPlugin("org.jetbrains.kotlin.plugin.scripting") { asd(project) }
        }
    }

    private fun asd(project: Project) {

//        project.extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>()

//        project.extensions.configure<KotlinProjectExtension> {
//            logger.lifecycle("configuring Kotlin Extension!!!")
//
//        }

        val kotlinExtension = project.extensions.findKotlinExtension()
        if (kotlinExtension == null) {
            logger.lifecycle("could not find Kotlin Extension")
            return
        }
        logger.lifecycle("Configuring Dokka in Gradle Kotlin Project ${project.path}")

        val dokka = project.extensions.getByType<DokkaPluginSettings>()

        /** Determine if a source set is 'main', and not test sources */
        fun KotlinSourceSet.isMainSourceSet(): Provider<Boolean> {

            fun allCompilations(): List<KotlinCompilation<*>> {
                return when (kotlinExtension) {
                    is KotlinMultiplatformExtension -> {
                        val allCompilations = kotlinExtension.targets.flatMap { target -> target.compilations }
                        return allCompilations.filter { compilation ->
                            this in compilation.allKotlinSourceSets || this == compilation.defaultSourceSet
                        }
                    }

                    is KotlinSingleTargetExtension<*> -> {
                        kotlinExtension.target.compilations.filter { compilation -> this in compilation.allKotlinSourceSets }
                    }

                    else -> emptyList()
                }
            }

            fun KotlinCompilation<*>.isMainCompilation(): Boolean {
                return try {
                    if (this is KotlinJvmAndroidCompilation) {
                        androidVariant is LibraryVariant || androidVariant is ApplicationVariant
                    } else {
                        name == "main"
                    }
                } catch (e: NoSuchMethodError) {
                    // Kotlin Plugin version below 1.4
                    !name.toLowerCase().endsWith("test")
                }
            }

            return project.provider {
                val compilations = allCompilations()
                compilations.isEmpty() || compilations.any { compilation -> compilation.isMainCompilation() }
            }
        }


        val kotlinTarget = project.provider {
            when (kotlinExtension) {
                is KotlinMultiplatformExtension -> {
                    kotlinExtension.targets
                        .map { it.platformType }
                        .singleOrNull()
                        ?: KotlinPlatformType.common
                }

                is KotlinSingleTargetExtension<*> -> {
                    kotlinExtension.target.platformType
                }

                else -> KotlinPlatformType.common
            }
        }

        val dokkaAnalysisPlatform = kotlinTarget.map { target -> Platform.fromString(target.name) }

        kotlinExtension.sourceSets.all {
            val kotlinSourceSet = this

            logger.lifecycle("auto configuring Kotlin Source Set ${kotlinSourceSet.name}")

            // TODO: Needs to respect filters.
            //  We probably need to change from "sourceRoots" to support "sourceFiles"
            //  https://github.com/Kotlin/dokka/issues/1215
            val sourceRoots = kotlinSourceSet.kotlin.sourceDirectories.filter { it.exists() }
//            val sourceRoots = kotlinSourceSet.kotlin.sourceDirectories.

            logger.lifecycle("kotlin source set ${kotlinSourceSet.name} has source roots: ${sourceRoots.map { it.invariantSeparatorsPath }}")

//            val dependentSourceSetNames = project.provider {
//                kotlinSourceSet.dependsOn.map {otherSrcSet ->
//                    objects.newInstance<DokkaSourceSetIDGradleBuilder>().apply {
//                        this.sourceSetName
//                    }
//                    DokkaSourceSetID("TODO add scope ID", it.name)
//                }.toSet()
//            }

            dokka.dokkaSourceSets.register(kotlinSourceSet.name) {
                this.suppress.set(kotlinSourceSet.isMainSourceSet())
                this.sourceRoots.from(sourceRoots)

                // need to check for resolution, because testImplementation can't be resolved....
                // so as a workaround, just manually check if this can be resolved.
                // maybe make a special, one-off, resolvable configuration?
                val implConf = project.configurations.named(kotlinSourceSet.implementationConfigurationName)
                if (implConf.get().isCanBeResolved) {
                    this.classpath.from(implConf)
                }

                this.analysisPlatform.set(dokkaAnalysisPlatform)

                kotlinSourceSet.dependsOn.forEach {
                    this.dependentSourceSets.register(it.name + dependentSourceSets.size) {
                        this.sourceSetName = it.name
                    }

                }
//                this.dependentSourceSets.addAllLater(dependentSourceSetNames)
//                this.dependentSourceSets.set(dependentSourceSetNames) // TODO fix dependent source sets
                this.displayName.set(kotlinTarget.map { target ->
                    kotlinSourceSet.name.substringBeforeLast(
                        delimiter = "Main",
                        missingDelimiterValue = target.name
                    )
                })
            }
        }
    }

    companion object {

        private fun ExtensionContainer.findKotlinExtension(): KotlinProjectExtension? =
            try {
                findByType<KotlinProjectExtension>()
            } catch (e: Throwable) {
                when (e) {
                    is TypeNotPresentException,
                    is ClassNotFoundException,
                    is NoClassDefFoundError -> null

                    else -> throw e
                }
            } ?: try {
                findByType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>()
            } catch (e: Throwable) {
                when (e) {
                    is TypeNotPresentException,
                    is ClassNotFoundException,
                    is NoClassDefFoundError -> null

                    else -> throw e
                }
            }
    }
}
