package org.jetbrains.conventions

import org.gradle.kotlin.dsl.support.serviceOf

/**
 * Utility for downloading and installing a Maven binary.
 */

plugins {
    base
}

abstract class SetupMavenProperties {
    val mavenVersion = "3.5.0"
    val mavenPluginToolsVersion = "3.5.2"
    abstract val mavenBuildDir: DirectoryProperty
    abstract val mavenBinDir: DirectoryProperty
    abstract val mvn: RegularFileProperty
}

val setupMavenProperties =
    extensions.create("setupMavenProperties", SetupMavenProperties::class).apply {
        mavenBuildDir.set(layout.buildDirectory.dir("maven"))
        mavenBinDir.set(layout.buildDirectory.dir("maven-bin"))
        mvn.set(mavenBinDir.map { it.file("apache-maven-$mavenVersion/bin/mvn") })
    }

val mavenBinary by configurations.registering {
    description = "used to download the Maven binary"
    isCanBeResolved = true
    isCanBeConsumed = false
    isVisible = false

    defaultDependencies {
        add(
            project.dependencies.create(
                group = "org.apache.maven",
                name = "apache-maven",
                version = setupMavenProperties.mavenVersion,
                classifier = "bin",
                ext = "zip"
            )
        )
    }
}

tasks.clean {
    delete(setupMavenProperties.mavenBuildDir)
    delete(setupMavenProperties.mavenBinDir)
}

val installMavenBinary by tasks.registering(Sync::class) {
    val archives = serviceOf<ArchiveOperations>()
    from(
        mavenBinary.flatMap { conf ->
            conf.incoming.artifacts.resolvedArtifacts.map { artifacts ->
                artifacts.map { archives.zipTree(it.file) }
            }
        }
    )
    into(setupMavenProperties.mavenBinDir)
}
