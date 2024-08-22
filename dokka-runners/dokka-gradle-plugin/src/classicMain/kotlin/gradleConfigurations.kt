/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.named
import org.jetbrains.dokka.gradle.internal.PluginFeaturesService.Companion.pluginFeaturesService

internal fun Project.maybeCreateDokkaDefaultPluginConfiguration(): Configuration {
    return configurations.maybeCreate("dokkaPlugin") {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
        isCanBeConsumed = false
    }
}

internal fun Project.maybeCreateDokkaDefaultRuntimeConfiguration(): Configuration {
    return configurations.maybeCreate("dokkaRuntime") {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
        isCanBeConsumed = false
    }
}

internal fun Project.maybeCreateDokkaPluginConfiguration(
    dokkaTaskName: String,
    additionalDependencies: Collection<Dependency> = emptySet()
): Configuration {
    return project.configurations.maybeCreate("${dokkaTaskName}Plugin") {
        extendsFrom(maybeCreateDokkaDefaultPluginConfiguration())
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
        isCanBeConsumed = false
        dependencies.add(
            if (project.pluginFeaturesService.enableK2Analysis) {
                project.dokkaArtifacts.analysisKotlinSymbols
            } else {
                project.dokkaArtifacts.analysisKotlinDescriptors
            }
        )
        dependencies.add(project.dokkaArtifacts.dokkaBase)
        dependencies.addAll(additionalDependencies)
    }
}

internal fun Project.maybeCreateDokkaRuntimeConfiguration(dokkaTaskName: String): Configuration {
    return project.configurations.maybeCreate("${dokkaTaskName}Runtime") {
        extendsFrom(maybeCreateDokkaDefaultRuntimeConfiguration())
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
        isCanBeConsumed = false
        defaultDependencies {
            add(project.dokkaArtifacts.dokkaCore)
        }
    }
}
