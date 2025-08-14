/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.named
import org.jetbrains.dokka.gradle.internal.PluginFeaturesService.Companion.pluginFeaturesService

internal fun Project.maybeCreateDokkaDefaultPluginConfiguration(): Configuration {
    return configurations.findOrCreate("dokkaPlugin") {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
        isCanBeConsumed = false
    }
}

internal fun Project.maybeCreateDokkaDefaultRuntimeConfiguration(): Configuration {
    return configurations.findOrCreate("dokkaRuntime") {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
        isCanBeConsumed = false
    }
}

internal fun Project.maybeCreateDokkaPluginConfiguration(
    dokkaTaskName: String,
    additionalDependencies: Collection<Dependency> = emptySet()
): Configuration {
    return project.configurations.findOrCreate("${dokkaTaskName}Plugin") {
        extendsFrom(maybeCreateDokkaDefaultPluginConfiguration())
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
        isCanBeConsumed = false

        dependencies.add(project.dokkaArtifacts.dokkaBase)
        dependencies.addAll(additionalDependencies)
    }
}

internal fun Project.maybeCreateDokkaRuntimeConfiguration(dokkaTaskName: String): Configuration {
    return project.configurations.findOrCreate("${dokkaTaskName}Runtime") {
        extendsFrom(maybeCreateDokkaDefaultRuntimeConfiguration())
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
        isCanBeConsumed = false
        defaultDependencies {
            dependencies.add(
                // Analysis dependencies are not a plugin
                // It should precede the core dependency in order
                // to use the shadowed stdlib from the analysis dependencies
                if (project.pluginFeaturesService.enableK2Analysis) {
                    project.dokkaArtifacts.analysisKotlinSymbols
                } else {
                    project.dokkaArtifacts.analysisKotlinDescriptors
                }
            )
            add(project.dokkaArtifacts.dokkaCore)
        }
    }
}

private fun <T : Any> NamedDomainObjectContainer<T>.findOrCreate(name: String, configuration: T.() -> Unit): T {
    return findByName(name) ?: create(name, configuration)
}
