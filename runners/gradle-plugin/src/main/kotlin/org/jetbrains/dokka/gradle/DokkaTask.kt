package org.jetbrains.dokka.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.jetbrains.dokka.DokkaBootstrapImpl
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.build

abstract class DokkaTask : AbstractDokkaTask(DokkaBootstrapImpl::class) {

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

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        return DokkaConfigurationImpl(
            moduleName = moduleName.getSafe(),
            moduleVersion = moduleVersion.orNull,
            outputDir = outputDirectory.getSafe(),
            cacheRoot = cacheRoot.getSafe(),
            offlineMode = offlineMode.getSafe(),
            failOnWarning = failOnWarning.getSafe(),
            sourceSets = unsuppressedSourceSets.build(),
            pluginsConfiguration = pluginsConfiguration.getSafe(),
            pluginsClasspath = plugins.resolve().toList()
        )
    }
}
