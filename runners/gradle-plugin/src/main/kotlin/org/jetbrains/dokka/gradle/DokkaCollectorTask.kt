package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.DokkaConfigurationImpl

open class DokkaCollectorTask : AbstractDokkaParentTask() {

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        val initialDokkaConfiguration = DokkaConfigurationImpl(
            outputDir = outputDirectory,
            cacheRoot = cacheRoot,
            failOnWarning = failOnWarning,
            offlineMode = offlineMode,
            pluginsClasspath = plugins.resolve().toList(),
        )

        val subprojectDokkaConfigurations = dokkaTasks.map { dokkaTask -> dokkaTask.buildDokkaConfiguration() }
        return subprojectDokkaConfigurations.fold(initialDokkaConfiguration) { acc, it: DokkaConfigurationImpl ->
            acc.copy(
                sourceSets = acc.sourceSets + it.sourceSets,
                pluginsClasspath = acc.pluginsClasspath + it.pluginsClasspath
            )
        }
    }
}
