package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.DokkaModuleConfigurationImpl

abstract class DokkaCollectorTask : AbstractDokkaParentTask() {

    override fun generateDocumentation() {
        checkChildDokkaTasksIsNotEmpty()
        super.generateDocumentation()
    }

    override fun buildDokkaModuleConfiguration(): DokkaModuleConfigurationImpl {
        val initialDokkaModuleConfiguration = DokkaModuleConfigurationImpl(
            moduleName = moduleName.getSafe(),
            outputDir = outputDirectory.getSafe(),
            cacheRoot = cacheRoot.getSafe(),
            failOnWarning = failOnWarning.getSafe(),
            offlineMode = offlineMode.getSafe(),
            pluginsClasspath = plugins.resolve().toList(),
        )

        val subprojectDokkaModuleConfigurations = childDokkaTasks.map { dokkaTask -> dokkaTask.buildDokkaModuleConfiguration() }
        return subprojectDokkaModuleConfigurations.fold(initialDokkaModuleConfiguration) { acc, it: DokkaModuleConfigurationImpl ->
            acc.copy(
                sourceSets = acc.sourceSets + it.sourceSets,
                pluginsClasspath = acc.pluginsClasspath + it.pluginsClasspath
            )
        }
    }
}
