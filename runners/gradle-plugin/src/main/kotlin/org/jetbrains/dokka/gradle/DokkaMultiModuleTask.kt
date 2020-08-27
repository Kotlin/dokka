package org.jetbrains.dokka.gradle

import org.gradle.api.internal.tasks.TaskDependencyInternal
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import org.jetbrains.dokka.DokkaMultimoduleBootstrapImpl
import java.io.File

@Suppress("unused") // Shall provide source compatibility if possible
@Deprecated("Use 'DokkaMultimoduleTask' instead", ReplaceWith("DokkaMultimoduleTask"))
typealias DokkaMultimoduleTask = DokkaMultiModuleTask


abstract class DokkaMultiModuleTask : AbstractDokkaParentTask(DokkaMultimoduleBootstrapImpl::class) {

    /**
     * Name of the file containing all necessary module information.
     * This file has to be placed inside the subproject root directory.
     */
    @Internal
    val documentationFileName: Property<String> = project.objects.safeProperty<String>()
        .safeConvention("README.md")

    @Internal
    val fileLayout: Property<DokkaMultiModuleFileLayout> = project.objects.safeProperty<DokkaMultiModuleFileLayout>()
        .safeConvention(DokkaMultiModuleFileLayout.CompactInParent)

    @get:InputFiles
    internal val childDocumentationFiles: Iterable<File>
        get() = childDokkaTasks.map { task -> task.project.projectDir.resolve(documentationFileName.getSafe()) }

    @get:InputFiles
    internal val sourceChildOutputDirectories: Iterable<File>
        get() = childDokkaTasks.map { task -> task.outputDirectory.getSafe() }

    @get:OutputDirectories
    internal val targetChildOutputDirectories: Iterable<File>
        get() = childDokkaTasks.map { task -> targetChildOutputDirectory(task) }

    @Internal
    override fun getTaskDependencies(): TaskDependencyInternal =
        super.getTaskDependencies() + childDokkaTasks


    override fun generateDocumentation() {
        checkChildDokkaTasksIsNotEmpty()
        copyChildOutputDirectories()
        super.generateDocumentation()
    }

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl = DokkaConfigurationImpl(
        moduleName = moduleName.getSafe(),
        outputDir = outputDirectory.getSafe(),
        cacheRoot = cacheRoot.getSafe(),
        pluginsConfiguration = pluginsConfiguration.getSafe(),
        failOnWarning = failOnWarning.getSafe(),
        offlineMode = offlineMode.getSafe(),
        pluginsClasspath = plugins.resolve().toList(),
        modules = childDokkaTasks.map { dokkaTask ->
            DokkaModuleDescriptionImpl(
                name = dokkaTask.project.name,
                path = targetChildOutputDirectory(dokkaTask).relativeTo(outputDirectory.getSafe()),
                docFile = dokkaTask.project.projectDir.resolve(documentationFileName.get()).absoluteFile
            )
        }
    )
}

