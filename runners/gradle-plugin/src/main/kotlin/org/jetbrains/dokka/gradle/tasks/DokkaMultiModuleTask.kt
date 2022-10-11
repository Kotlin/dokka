package org.jetbrains.dokka.gradle.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.internal.tasks.TaskDependencyInternal
import org.gradle.api.provider.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import org.jetbrains.dokka.gradle.DokkaMultiModuleFileLayout
import org.jetbrains.dokka.gradle.tasks.*
import org.jetbrains.dokka.gradle.*
import java.io.File

@Suppress("unused") // Shall provide source compatibility if possible
@Deprecated("Use 'DokkaMultimoduleTask' instead", ReplaceWith("DokkaMultimoduleTask"))
typealias DokkaMultimoduleTask = DokkaMultiModuleTask

private typealias TaskPath = String

@CacheableTask
abstract class DokkaMultiModuleTask : AbstractDokkaParentTask() {
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includes: ConfigurableFileCollection

    @Internal
    val fileLayout: Property<DokkaMultiModuleFileLayout> = project.objects.safeProperty<DokkaMultiModuleFileLayout>()
        .safeConvention(DokkaMultiModuleFileLayout.CompactInParent)

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal abstract val sourceChildOutputDirectories: ConfigurableFileCollection

    @get:OutputDirectories
    internal val targetChildOutputDirectories: Iterable<File>
        get() = childDokkaTasks.map { task -> targetChildOutputDirectory(task) }

    @get:Input
    internal val childDokkaTaskIncludes: Map<TaskPath, Set<File>>
        get() = childDokkaTasks.filterIsInstance<DokkaTaskPartial>().associate { task ->
            task.path to task.dokkaSourceSets.flatMap { it.includes }.toSet()
        }

    @Internal
    override fun getTaskDependencies(): TaskDependencyInternal =
        super.getTaskDependencies() + childDokkaTasks


    override fun generateDocumentation() {
        checkChildDokkaTasksIsNotEmpty()
        super.generateDocumentation()
    }

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        return DokkaConfigurationImpl(
            moduleName = moduleName.getSafe(),
            moduleVersion = moduleVersion.getValidVersionOrNull(),
            outputDir = outputDirectory.asFile.get(),
            cacheRoot = cacheRoot.asFile.get(),
            pluginsConfiguration = buildPluginsConfiguration(),
            failOnWarning = failOnWarning.getSafe(),
            offlineMode = offlineMode.getSafe(),
            pluginsClasspath = plugins.resolve().toList(),
            modules = childDokkaTasks.map { dokkaTask ->
                DokkaModuleDescriptionImpl(
                    name = dokkaTask.moduleName.getSafe(),
                    relativePathToOutputDirectory = targetChildOutputDirectory(dokkaTask).relativeTo(outputDirectory.asFile.get()),
                    includes = childDokkaTaskIncludes[dokkaTask.path].orEmpty(),
                    sourceOutputDirectory = dokkaTask.outputDirectory.asFile.get(),
                )
            },
            includes = includes.toSet(),
        )
    }
}

