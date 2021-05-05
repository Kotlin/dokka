package org.jetbrains.dokka.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.tasks.*
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.build

@CacheableTask
abstract class DokkaTaskPartial : AbstractDokkaLeafTask() {

    /**
     * Only contains source sets that are marked with `isDocumented`.
     * Non documented source sets are not relevant for Gradle's UP-TO-DATE mechanism, as well
     * as task dependency graph.
     */
    @get:Nested
    protected val unsuppressedSourceSets: List<GradleDokkaSourceSetBuilder>
        get() = dokkaSourceSets
            .toList()
            .also(::checkSourceSetDependencies)
            .filterNot { it.suppress.getSafe() }

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        return DokkaConfigurationImpl(
            moduleName = moduleName.getSafe(),
            moduleVersion = moduleVersion.orNull,
            outputDir = outputDirectory.getSafe(),
            cacheRoot = cacheRoot.getSafe(),
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