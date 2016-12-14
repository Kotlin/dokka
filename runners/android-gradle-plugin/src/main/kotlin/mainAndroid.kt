package org.jetbrains.dokka.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.VariantManager
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import java.io.File
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.memberProperties

open class DokkaAndroidPlugin : Plugin<Project> {
    val allVariantsClassPath = mutableSetOf<File>()

    override fun apply(project: Project) {

        DokkaVersion.loadFrom(javaClass.getResourceAsStream("/META-INF/gradle-plugins/org.jetbrains.dokka-android.properties"))
        project.tasks.create("dokka", DokkaAndroidTask::class.java).apply {
            moduleName = project.name
            outputDirectory = File(project.buildDir, "dokka").absolutePath
        }

        if (project.hasAndroidPlugin()) {
            project.afterEvaluate {
                collectClasspath(project)
            }
        }
        else {
            project.plugins.whenPluginAdded {
                if (project.hasAndroidPlugin()) {
                    collectClasspath(project)
                }
            }
        }
    }

    private fun collectClasspath(project: Project) {
        val plugin = (project.plugins.findPlugin("android")
                ?: project.plugins.findPlugin("android-library")
                ?: project.plugins.findPlugin("com.android.test")) as BasePlugin

        val variantManagerProperty = plugin::class.memberProperties.find { it.name == "variantManager" }!!
        variantManagerProperty.isAccessible = true
        val variantManager = variantManagerProperty.get(plugin) as VariantManager
        variantManager.variantDataList.flatMapTo(allVariantsClassPath) { it.variantConfiguration.compileClasspath }
    }
}

open class DokkaAndroidTask : DokkaTask() {
    override val sdkProvider: SdkProvider? = AndroidSdkProvider(project)
}

private fun Project.hasAndroidPlugin() = plugins.hasPlugin("com.android.library") || plugins.hasPlugin("com.android.application")

private fun Project.findDokkaAndroidPlugin() = plugins.findPlugin(DokkaAndroidPlugin::class.java)

private fun Project.collectAllVariants(): Collection<BaseVariant> {
    extensions.findByType(LibraryExtension::class.java)?.let {
        return it.libraryVariants
    }
    extensions.findByType(AppExtension::class.java)?.let {
        return it.applicationVariants
    }
    return emptyList()
}

private class AndroidSdkProvider(private val project: Project) : SdkProvider {
    private val ext by lazy {
        project.extensions.findByType(LibraryExtension::class.java) ?: project.extensions.findByType(AppExtension::class.java)
    }

    override val name: String = "android"

    override val isValid: Boolean
        get() = project.hasAndroidPlugin()

    override val classpath: List<File>
        get() = ext.bootClasspath + (project.findDokkaAndroidPlugin()?.allVariantsClassPath ?: emptyList<File>())

    override val sourceDirs: Set<File>?
        get() {
            val sourceSet = ext?.sourceSets?.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
            return sourceSet?.java?.srcDirs
        }
}
