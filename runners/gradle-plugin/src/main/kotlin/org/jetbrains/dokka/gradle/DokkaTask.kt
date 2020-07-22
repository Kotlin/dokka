package org.jetbrains.dokka.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.tasks.Nested
import org.jetbrains.dokka.DokkaBootstrapImpl
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.build

open class DokkaTask : AbstractDokkaTask(DokkaBootstrapImpl::class) {

    @get:Nested
    val dokkaSourceSets: NamedDomainObjectContainer<GradleDokkaSourceSetBuilder> =
        project.container(GradleDokkaSourceSetBuilder::class.java, GradleDokkaSourceSetBuilderFactory())
            .also { container ->
                DslObject(this).extensions.add("dokkaSourceSets", container)
                project.findKotlinSourceSets().orEmpty().forEach { kotlinSourceSet ->
                    container.register(kotlinSourceSet.name) { dokkaSourceSet ->
                        dokkaSourceSet.configureWithKotlinSourceSetGist(kotlinSourceSet)
                    }
                }
            }

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        return DokkaConfigurationImpl(
            outputDir = outputDirectory,
            cacheRoot = cacheRoot,
            offlineMode = offlineMode,
            failOnWarning = failOnWarning,
            sourceSets = dokkaSourceSets.build(),
            pluginsConfiguration = pluginsConfiguration,
            pluginsClasspath = plugins.resolve()
        )
    }
}
