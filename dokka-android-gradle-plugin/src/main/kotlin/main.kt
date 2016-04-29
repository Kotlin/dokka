package org.jetbrains.dokka.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import java.io.File

open class DokkaAndroidPlugin : Plugin<Project> {
    override fun apply(project: Project) {
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
    private val ext by lazy {
        project.extensions.findByType(LibraryExtension::class.java) ?: project.extensions.findByType(AppExtension::class.java)
    }

    override val name: String = "android"

    override val isValid: Boolean
        get() = project.plugins.hasPlugin("com.android.library") || project.plugins.hasPlugin("com.android.application")

    override val classpath: List<File>
        get() = ext.bootClasspath

    override val sourceDirs: Set<File>?
        get() {
            val sourceSet = ext?.sourceSets?.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
            return sourceSet?.java?.srcDirs
        }
}
