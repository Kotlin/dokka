package org.jetbrains.dokka.gradle

import com.android.build.gradle.*
import com.android.build.gradle.internal.VariantManager
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import java.io.File
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.memberProperties

open class DokkaAndroidPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        DokkaVersion.loadFrom(javaClass.getResourceAsStream("/META-INF/gradle-plugins/org.jetbrains.dokka-android.properties"))
        project.tasks.create("dokka", DokkaAndroidTask::class.java).apply {
            moduleName = project.name
            outputDirectory = File(project.buildDir, "dokka").absolutePath
        }
    }
}

open class DokkaAndroidTask : DokkaTask() {
    override val sdkProvider: SdkProvider? = AndroidSdkProvider(project)
}

private class AndroidSdkProvider(private val project: Project) : SdkProvider {
    private val ext: BaseExtension? by lazy {
        project.extensions.findByType(LibraryExtension::class.java)
                ?: project.extensions.findByType(AppExtension::class.java)
                ?: project.extensions.findByType(TestExtension::class.java)
    }

    private val isAndroidProject: Boolean get() = ext != null

    private val variantManager: VariantManager? by lazy {
        val plugin = (project.plugins.findPlugin("android")
                ?: project.plugins.findPlugin("android-library")
                ?: project.plugins.findPlugin("com.android.test")
                ?: throw Exception("Android plugin not found, please use dokka-android with android or android-library plugin.")) as BasePlugin
        plugin.javaClass.kotlin.memberProperties
                .find { it.name == "variantManager" }
                ?.apply { isAccessible = true }
                ?.let { it.get(plugin) as VariantManager }
                ?: plugin.variantManager
    }

    private val allVariantsClassPath by lazy {
        try {
            variantManager?.variantDataList?.flatMap { it.variantConfiguration.compileClasspath }!!
        } catch(e: Exception) {
            throw Exception("Unsupported version of android build tools, could not access variant manager.", e)
        }
    }

    override val name: String = "android"

    override val isValid: Boolean
        get() = isAndroidProject

    override val classpath: List<File>
        get() = ext?.bootClasspath.orEmpty() + allVariantsClassPath

    override val sourceDirs: Set<File>?
        get() {
            val sourceSet = ext?.sourceSets?.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
            return sourceSet?.java?.srcDirs
        }
}
