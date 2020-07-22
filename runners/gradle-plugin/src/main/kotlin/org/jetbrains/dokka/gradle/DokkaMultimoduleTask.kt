package org.jetbrains.dokka.gradle

import org.gradle.api.internal.tasks.TaskDependencyInternal
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import org.jetbrains.dokka.DokkaMultimoduleBootstrapImpl
import org.jetbrains.dokka.plugability.Configurable

open class DokkaMultimoduleTask : AbstractDokkaParentTask(DokkaMultimoduleBootstrapImpl::class), Configurable {

    /**
     * Name of the file containing all necessary module information.
     * This file has to be placed inside the subrpojects root directory.
     */
    @Input
    var documentationFileName: String = "README.md"

    @Internal
    override fun getTaskDependencies(): TaskDependencyInternal {
        return super.getTaskDependencies() + dokkaTasks
    }

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        return DokkaConfigurationImpl(
            outputDir = outputDirectory,
            cacheRoot = cacheRoot,
            pluginsConfiguration = pluginsConfiguration,
            failOnWarning = failOnWarning,
            offlineMode = offlineMode,
            pluginsClasspath = plugins.resolve().toSet(),
            modules = dokkaTasks.map { dokkaTask ->
                DokkaModuleDescriptionImpl(
                    name = dokkaTask.project.name,
                    path = dokkaTask.outputDirectory.relativeTo(outputDirectory),
                    docFile = dokkaTask.project.projectDir.resolve(documentationFileName).absoluteFile
                )
            }
        )
    }
}
