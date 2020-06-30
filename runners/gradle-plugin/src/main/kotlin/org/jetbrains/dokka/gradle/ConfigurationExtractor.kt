package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.dokka.ReflectDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.io.Serializable

class ConfigurationExtractor(private val project: Project) {

    fun extractConfiguration(sourceSetName: String): PlatformData? {
        val projectExtension = project.extensions.getByType(KotlinProjectExtension::class.java)
        val sourceSet = projectExtension.sourceSets.findByName(sourceSetName)
            ?: run { project.logger.error("No source set with name '$sourceSetName' found"); return null }
        val compilation = try {
            when (projectExtension) {
                is KotlinMultiplatformExtension -> {
                    val targets = projectExtension.targets.flatMap { it.compilations }
                    targets.find { it.name == sourceSetName }
                        ?: targets.find { it.kotlinSourceSets.contains(sourceSet) }
                }
                is KotlinSingleTargetExtension -> projectExtension.target.compilations.find {
                    it.kotlinSourceSets.contains(sourceSet)
                }
                else -> null
            }
        } catch (e: NoClassDefFoundError) { // Old Kotlin plugin versions
            null
        }

        val sourceRoots = sourceSet.sourceFiles
        val classpath = compilation?.classpath
            ?: sourceRoots + sourceSet.allParentSourceFiles()

        return PlatformData(
            sourceSetName,
            classpath.filter { it.exists() },
            sourceRoots,
            sourceSet.dependsOn.map { it.name },
            compilation?.target?.platformType?.name ?: "common"
        )
    }

    private fun KotlinSourceSet.allParentSourceFiles(): List<File> =
        sourceFiles + dependsOn.flatMap { it.allParentSourceFiles() }

    fun extractFromJavaPlugin(): PlatformData? =
        project.convention.findPlugin(JavaPluginConvention::class.java)
            ?.run { sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)?.allSource?.srcDirs }
            ?.let { PlatformData(null, emptyList(), it.toList(), emptyList(), "") }

    fun extractFromKotlinTasks(kotlinTasks: List<Task>): List<PlatformData> =
        try {
            kotlinTasks.map { extractFromKotlinTask(it) }
        } catch (e: Throwable) {
            when (e) {
                is UnknownDomainObjectException, is NoClassDefFoundError, is ClassNotFoundException ->
                    listOfNotNull(extractFromKotlinTasksTheHardWay(kotlinTasks))
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
                        .flatMap { it.compilations }.firstOrNull { it.compileKotlinTask == task }
                else -> throw e
            }
        }.let { compilation ->
            PlatformData(
                task.name,
                compilation?.classpath.orEmpty(),
                compilation?.sourceFiles.orEmpty(),
                compilation?.dependentSourceSets?.map { it.name }.orEmpty(),
                compilation?.platformType?.toString() ?: ""
            )
        }

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

        return PlatformData(null, classpath, allSourceRoots.toList(), emptyList(), "")
    }

    private val KotlinCompilation<*>.sourceFiles: List<File>
        get() = kotlinSourceSets.flatMap { it.sourceFiles }

    private val KotlinSourceSet.sourceFiles: List<File>
        get() = kotlin.sourceDirectories.filter { it.exists() }.toList()

    private val KotlinCompilation<*>.dependentSourceSets: Set<KotlinSourceSet>
        get() = (allKotlinSourceSets - kotlinSourceSets)

    private val KotlinCompilation<*>.classpath: List<File>
        get() = if (target.isAndroidTarget()) {
            getClasspathFromAndroidTask(this)
        } else {
            getClasspathFromRegularTask(this)
        }

    // This is a workaround for KT-33893
    private fun getClasspathFromAndroidTask(compilation: KotlinCompilation<*>): List<File> = (compilation
        .compileKotlinTask as? KotlinCompile)
        ?.classpath?.files?.toList() ?: getClasspathFromRegularTask(compilation)

    private fun getClasspathFromRegularTask(compilation: KotlinCompilation<*>): List<File> =
        compilation
            .compileDependencyFiles
            .files
            .toList()
            .filter { it.exists() }

    data class PlatformData(
        val name: String?,
        val classpath: List<File>,
        val sourceRoots: List<File>,
        val dependentSourceSets: List<String>,
        val platform: String
    ) : Serializable
}
