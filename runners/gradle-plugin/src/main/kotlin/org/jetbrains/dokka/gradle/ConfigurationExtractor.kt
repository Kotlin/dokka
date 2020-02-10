package org.jetbrains.dokka.gradle

import com.android.build.gradle.*
import com.android.build.gradle.api.BaseVariant
import com.android.builder.core.BuilderConstants
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.dokka.ReflectDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.io.File
import java.io.Serializable

class ConfigurationExtractor(private val project: Project) {

    fun extractConfiguration(targetName: String, variantNames: List<String>) =
        if (project.isMultiplatformProject()) {
            extractFromMultiPlatform(targetName, variantNames)
        } else {
            extractFromSinglePlatform(variantNames)
        }

    private fun extractFromSinglePlatform(variantNames: List<String>): PlatformData? {
        val target: KotlinTarget
        try {
            target = project.extensions.getByType(KotlinSingleTargetExtension::class.java).target
        } catch (e: Throwable) {
            when (e) {
                is UnknownDomainObjectException, is NoClassDefFoundError, is ClassNotFoundException ->
                    return null
                else -> throw e
            }
        }

        return try {
            PlatformData(
                null,
                accumulateClassPaths(variantNames, target),
                accumulateSourceSets(variantNames, target),
                getPlatformName(target.platformType)
            )
        } catch (e: NoSuchMethodError) {
            null
        }
    }

    private fun extractFromMultiPlatform(targetName: String, variantNames: List<String>): PlatformData? =
        try {
            project.extensions.getByType(KotlinMultiplatformExtension::class.java).targets
        } catch (e: Throwable) {
            when (e) {
                is UnknownDomainObjectException, is NoClassDefFoundError, is ClassNotFoundException ->
                    null
                else -> throw e
            }
        }?.let {
            val fixedName = if (targetName.toLowerCase() == "common") "metadata" else targetName.toLowerCase()
            it.find { target -> target.name.toLowerCase() == fixedName }?.let { target ->
                PlatformData(
                    fixedName,
                    accumulateClassPaths(variantNames, target),
                    accumulateSourceSets(variantNames, target),
                    target.platformType.toString()
                )
            }
        }

    fun extractFromJavaPlugin(): PlatformData? =
        project.convention.findPlugin(JavaPluginConvention::class.java)
            ?.run { sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)?.allSource?.srcDirs }
            ?.let { PlatformData(null, emptyList(), it.toList(), "") }

    fun extractFromKotlinTasks(kotlinTasks: List<Task>): PlatformData? =
        try {
            kotlinTasks.map { extractFromKotlinTask(it) }.let { platformDataList ->
                PlatformData(
                    null,
                    platformDataList.flatMap { it.classpath },
                    platformDataList.flatMap { it.sourceRoots },
                    ""
                )
            }
        } catch (e: Throwable) {
            when (e) {
                is UnknownDomainObjectException, is NoClassDefFoundError, is ClassNotFoundException ->
                    extractFromKotlinTasksTheHardWay(kotlinTasks)
                else -> throw e
            }
        }

    private fun extractFromKotlinTask(task: Task): PlatformData =
        try {
            project.extensions.getByType(KotlinSingleTargetExtension::class.java).target
                .compilations
                .find { it.compileKotlinTask == task }
        } catch (e: Throwable) {
            when (e) {
                is UnknownDomainObjectException, is NoClassDefFoundError, is ClassNotFoundException ->
                    project.extensions.getByType(KotlinMultiplatformExtension::class.java).targets
                        .firstNotNullResult { target -> target.compilations.find { it.compileKotlinTask == task } }
                else -> throw e
            }
        }.let { PlatformData(task.name, getClasspath(it), getSourceSet(it), it?.platformType?.toString() ?: "") }

    private fun extractFromKotlinTasksTheHardWay(kotlinTasks: List<Task>): PlatformData? {
        val allClasspath = mutableSetOf<File>()
        var allClasspathFileCollection: FileCollection = project.files()
        val allSourceRoots = mutableSetOf<File>()

        kotlinTasks.forEach {
            with(ReflectDsl) {
                val taskSourceRoots: List<File>
                val abstractKotlinCompileClz: Class<out Any>
                try {
                    taskSourceRoots = it["sourceRootsContainer"]["sourceRoots"].v()
                    abstractKotlinCompileClz = DokkaTask.getAbstractKotlinCompileFor(it)!!
                } catch (e: NullPointerException) {
                    println("Error during extraction of sources from kotlinTasks. This may be a result of outdated Kotlin Gradle Plugin")
                    return null
                }

                val taskClasspath: Iterable<File> =
                    (it["getClasspath", AbstractCompile::class].takeIfIsFunc()?.invoke()
                            ?: it["compileClasspath", abstractKotlinCompileClz].takeIfIsProp()?.v()
                            ?: it["getClasspath", abstractKotlinCompileClz]())

                if (taskClasspath is FileCollection) {
                    allClasspathFileCollection += taskClasspath
                } else {
                    allClasspath += taskClasspath
                }
                allSourceRoots += taskSourceRoots.filter { it.exists() }
            }
        }
        val classpath: MutableList<File> = try {
            allClasspathFileCollection.toMutableList()
        } catch (e: ResolveException) {
            mutableListOf()
        }
        classpath.addAll(project.files(allClasspath).toList())

        return PlatformData(null, classpath, allSourceRoots.toList(), "")
    }

    private fun getSourceSet(target: KotlinTarget, variantName: String): List<File> =
        if (target.isAndroidTarget())
            getSourceSet(getCompilation(target, variantName))
        else
            getSourceSet(getMainCompilation(target))

    private fun getClasspath(target: KotlinTarget, variantName: String): List<File> =
        if (target.isAndroidTarget())
            getClasspathFromAndroidTask(getCompilation(target, variantName))
        else
            getClasspath(getMainCompilation(target))

    private fun getSourceSet(compilation: KotlinCompilation<*>?): List<File> = compilation
        ?.allKotlinSourceSets
        ?.flatMap { it.kotlin.sourceDirectories }
        ?.filter { it.exists() }
        .orEmpty()

    private fun getClasspath(compilation: KotlinCompilation<*>?): List<File> = compilation
        ?.compileDependencyFiles
        ?.files
        ?.toList()
        ?.filter { it.exists() }
        .orEmpty()

    // This is a workaround for KT-33893
    private fun getClasspathFromAndroidTask(compilation: KotlinCompilation<*>): List<File> = (compilation
        .compileKotlinTask as? KotlinCompile)
        ?.classpath?.files?.toList() ?: getClasspath(compilation)

    private fun getMainCompilation(target: KotlinTarget) =
        getCompilation(target, getMainCompilationName(target))

    private fun getCompilation(target: KotlinTarget, name: String) =
        target.compilations.getByName(name)

    private fun getMainCompilationName(target: KotlinTarget) = if (target.isAndroidTarget())
        getVariants(project).filter { it.buildType.name == BuilderConstants.RELEASE }.map { it.name }.first()
    else
        KotlinCompilation.MAIN_COMPILATION_NAME

    private fun getVariants(project: Project): Set<BaseVariant> {
        val androidExtension = project.extensions.getByName("android")
        val baseVariants = when (androidExtension) {
            is AppExtension -> androidExtension.applicationVariants.toSet()
            is LibraryExtension -> {
                androidExtension.libraryVariants.toSet() +
                        if (androidExtension is FeatureExtension) {
                            androidExtension.featureVariants.toSet()
                        } else {
                            emptySet<BaseVariant>()
                        }
            }
            is TestExtension -> androidExtension.applicationVariants.toSet()
            else -> emptySet()
        }
        val testVariants = if (androidExtension is TestedExtension) {
            androidExtension.testVariants.toSet() + androidExtension.unitTestVariants.toSet()
        } else {
            emptySet<BaseVariant>()
        }

        return baseVariants + testVariants
    }

    private fun getPlatformName(platform: KotlinPlatformType): String =
        if (platform == KotlinPlatformType.androidJvm) KotlinPlatformType.jvm.toString() else platform.toString()

    private fun accumulateClassPaths(variantNames: List<String>, target: KotlinTarget) =
        if (variantNames.isNotEmpty()) {
            variantNames.flatMap { getClasspath(target, it) }.distinct()
        } else {
            if (target.isAndroidTarget())
                getClasspathFromAndroidTask(getMainCompilation(target))
            else
                getClasspath(getMainCompilation(target))
        }

    private fun accumulateSourceSets(variantNames: List<String>, target: KotlinTarget) =
        if (variantNames.isNotEmpty())
            variantNames.flatMap { getSourceSet(target, it) }.distinct()
        else
            getSourceSet(getMainCompilation(target))

    data class PlatformData(
        val name: String?,
        val classpath: List<File>,
        val sourceRoots: List<File>,
        val platform: String
    ) : Serializable
}