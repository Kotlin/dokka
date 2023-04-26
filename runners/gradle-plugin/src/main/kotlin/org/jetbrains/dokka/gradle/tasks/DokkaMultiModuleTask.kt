@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.dokka.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.internal.tasks.TaskDependencyInternal
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import java.io.File

@Suppress("unused") // Shall provide source compatibility if possible
@Deprecated("Use 'DokkaMultiModuleTask' instead", ReplaceWith("DokkaMultiModuleTask"), DeprecationLevel.ERROR)
typealias DokkaMultimoduleTask = DokkaMultiModuleTask

private typealias TaskPath = String

@CacheableTask
abstract class DokkaMultiModuleTask : AbstractDokkaParentTask() {

    /**
     * List of Markdown files that contain
     * [module and package documentation](https://kotlinlang.org/docs/dokka-module-and-package-docs.html).
     *
     * Contents of specified files will be parsed and embedded into documentation as module and package descriptions.
     *
     * Example of such a file:
     *
     * ```markdown
     * # Module kotlin-demo
     *
     * The module shows the Dokka usage.
     *
     * # Package org.jetbrains.kotlin.demo
     *
     * Contains assorted useful stuff.
     *
     * ## Level 2 heading
     *
     * Text after this heading is also part of documentation for `org.jetbrains.kotlin.demo`
     *
     * # Package org.jetbrains.kotlin.demo2
     *
     * Useful stuff in another package.
     * ```
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includes: ConfigurableFileCollection

    @Internal
    val fileLayout: Property<DokkaMultiModuleFileLayout> = project.objects.property<DokkaMultiModuleFileLayout>()
        .convention(DokkaMultiModuleFileLayout.CompactInParent)

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal abstract val sourceChildOutputDirectories: ConfigurableFileCollection

    @get:OutputDirectories
    internal val targetChildOutputDirectories: Provider<Iterable<Directory>> = project.provider {
        childDokkaTasks.map { task -> targetChildOutputDirectory(task).get() }
    }

    @get:Input
    internal val childDokkaTaskIncludes: Map<TaskPath, Set<File>>
        get() = childDokkaTasks.filterIsInstance<DokkaTaskPartial>().associate { task ->
            task.path to task.dokkaSourceSets.flatMap { it.includes }.toSet()
        }

    // The method contains a reference to internal Gradle API that is nice not to use.
    // There was an attempt to get rid of it, but it was not successful
    // See: https://github.com/Kotlin/dokka/pull/2835
    @Internal
    override fun getTaskDependencies(): TaskDependencyInternal =
        super.getTaskDependencies() + childDokkaTasks


    override fun generateDocumentation() {
        checkChildDokkaTasksIsNotEmpty()
        super.generateDocumentation()
    }

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        return DokkaConfigurationImpl(
            moduleName = moduleName.get(),
            moduleVersion = moduleVersion.getValidVersionOrNull(),
            outputDir = outputDirectory.asFile.get(),
            cacheRoot = cacheRoot.asFile.orNull,
            pluginsConfiguration = buildPluginsConfiguration(),
        failOnWarning = failOnWarning.get(),
        offlineMode = offlineMode.get(),
            pluginsClasspath = plugins.resolve().toList(),
            modules = childDokkaTasks.map { dokkaTask ->
                DokkaModuleDescriptionImpl(
                    name = dokkaTask.moduleName.get(),
                    relativePathToOutputDirectory = targetChildOutputDirectory(dokkaTask).get().asFile.relativeTo(
                        outputDirectory.asFile.get()
                    ),
                    includes = childDokkaTaskIncludes[dokkaTask.path].orEmpty(),
                    sourceOutputDirectory = dokkaTask.outputDirectory.asFile.get(),
                )
            },
            includes = includes.toSet(),
        )
    }
}
