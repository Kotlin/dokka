@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.dokka.gradle

import org.gradle.api.tasks.*
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.build

@CacheableTask
abstract class DokkaTaskPartial : AbstractDokkaLeafTask() {

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        return DokkaConfigurationImpl(
            moduleName = moduleName.getSafe(),
            moduleVersion = moduleVersion.orNull,
            outputDir = outputDirectory.asFile.get(),
            cacheRoot = cacheRoot.asFile.orNull,
            offlineMode = offlineMode.getSafe(),
            failOnWarning = failOnWarning.getSafe(),
            sourceSets = unsuppressedSourceSets.build(),
            pluginsConfiguration = buildPluginsConfiguration(),
            pluginsClasspath = plugins.resolve().toList(),
            delayTemplateSubstitution = true,
            suppressObviousFunctions = suppressObviousFunctions.getSafe(),
            suppressInheritedMembers = suppressInheritedMembers.getSafe(),
        )
    }
}
