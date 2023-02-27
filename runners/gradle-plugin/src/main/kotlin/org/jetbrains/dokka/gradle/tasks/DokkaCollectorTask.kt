@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.dokka.gradle

import org.gradle.api.tasks.CacheableTask
import org.jetbrains.dokka.DokkaConfigurationImpl

@CacheableTask
abstract class DokkaCollectorTask : AbstractDokkaParentTask() {

    override fun generateDocumentation() {
        checkChildDokkaTasksIsNotEmpty()
        super.generateDocumentation()
    }

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        val initialDokkaConfiguration = DokkaConfigurationImpl(
            moduleName = moduleName.get(),
            outputDir = outputDirectory.asFile.get(),
            cacheRoot = cacheRoot.asFile.orNull,
            failOnWarning = failOnWarning.get(),
            offlineMode = offlineMode.get(),
            pluginsClasspath = plugins.resolve().toList(),
            pluginsConfiguration = buildPluginsConfiguration(),
            suppressObviousFunctions = suppressObviousFunctions.get(),
            suppressInheritedMembers = suppressInheritedMembers.get(),
        )

        val subprojectDokkaConfigurations = childDokkaTasks.map { dokkaTask -> dokkaTask.buildDokkaConfiguration() }
        return subprojectDokkaConfigurations.fold(initialDokkaConfiguration) { acc, it: DokkaConfigurationImpl ->
            acc.copy(
                sourceSets = acc.sourceSets + it.sourceSets,
                pluginsClasspath = acc.pluginsClasspath + it.pluginsClasspath
            )
        }
    }
}
