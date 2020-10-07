package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.DokkaConfigurationImpl

abstract class DokkaCollectorTask : AbstractDokkaParentTask() {

    override fun generateDocumentation() {
        checkChildDokkaTasksIsNotEmpty()
        super.generateDocumentation()
    }

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        val initialDokkaConfiguration = DokkaConfigurationImpl(
            moduleName = moduleName.getSafe(),
            outputDir = outputDirectory.getSafe(),
            cacheRoot = cacheRoot.getSafe(),
            failOnWarning = failOnWarning.getSafe(),
            offlineMode = offlineMode.getSafe(),
            pluginsClasspath = plugins.resolve().toList(),
            pluginsConfiguration = buildPluginsConfiguration()
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
