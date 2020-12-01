package org.jetbrains.dokka.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.jetbrains.dokka.*

abstract class DokkaTask : AbstractDokkaTask() {

    @get:Internal
    val dokkaSourceSets: NamedDomainObjectContainer<GradleDokkaSourceSetBuilder> =
        project.container(GradleDokkaSourceSetBuilder::class.java, gradleDokkaSourceSetBuilderFactory())
            .also { container ->
                DslObject(this).extensions.add("dokkaSourceSets", container)
                project.kotlinOrNull?.sourceSets?.all { kotlinSourceSet ->
                    container.register(kotlinSourceSet.name) { dokkaSourceSet ->
                        dokkaSourceSet.configureWithKotlinSourceSet(kotlinSourceSet)
                    }
                }
            }

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

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl =
        DokkaConfigurationImpl(
            moduleName = moduleName.getSafe(),
            moduleVersion = moduleVersion.orNull?.takeIf { it != "unspecified" },
            outputDir = outputDirectory.getSafe(),
            cacheRoot = cacheRoot.getSafe(),
            offlineMode = offlineMode.getSafe(),
            failOnWarning = failOnWarning.getSafe(),
            sourceSets = unsuppressedSourceSets.build(),
            pluginsConfiguration = buildPluginsConfiguration(),
            pluginsClasspath = plugins.resolve().toList()
        )
}
