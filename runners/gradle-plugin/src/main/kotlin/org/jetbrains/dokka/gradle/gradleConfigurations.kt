package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Usage

internal fun Project.maybeCreateDokkaDefaultPluginConfiguration(): Configuration {
    return configurations.maybeCreate("dokkaPlugin") {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, "java-runtime"))
        isCanBeConsumed = false
    }
}

internal fun Project.maybeCreateDokkaDefaultRuntimeConfiguration(): Configuration {
    return configurations.maybeCreate("dokkaRuntime") {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, "java-runtime"))
        isCanBeConsumed = false
    }
}

internal fun Project.maybeCreateDokkaPluginConfiguration(dokkaTaskName: String, additionalDependencies: Collection<Dependency> = emptySet()): Configuration {
    return project.configurations.maybeCreate("${dokkaTaskName}Plugin") {
        extendsFrom(maybeCreateDokkaDefaultPluginConfiguration())
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, "java-runtime"))
        isCanBeConsumed = false
        dependencies.add(project.dokkaArtifacts.dokkaBase)
        dependencies.addAll(additionalDependencies)
    }
}

internal fun Project.maybeCreateDokkaRuntimeConfiguration(dokkaTaskName: String): Configuration {
    return project.configurations.maybeCreate("${dokkaTaskName}Runtime") {
        extendsFrom(maybeCreateDokkaDefaultRuntimeConfiguration())
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, "java-runtime"))
        isCanBeConsumed = false
        defaultDependencies { dependencies ->
            dependencies.add(project.dokkaArtifacts.dokkaCore)
        }
    }
}
