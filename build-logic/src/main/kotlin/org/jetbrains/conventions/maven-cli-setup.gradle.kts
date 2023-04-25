package org.jetbrains.conventions

import org.gradle.kotlin.dsl.support.serviceOf

/**
 * Utility for downloading and installing a Maven binary.
 *
 * Provides the `setupMavenProperties` extension that contains the default versions and locations
 * of the Maven binary.
 *
 * The task [installMavenBinary] will download and unzip the Maven bianry.
 */

plugins {
    base
}

abstract class MavenCliSetupExtension {
    abstract val mavenVersion: Property<String>
    abstract val mavenPluginToolsVersion: Property<String>
    abstract val mavenBuildDir: DirectoryProperty

    /** Directory that will contain the unpacked Apache Maven dependency */
    abstract val mavenInstallDir: DirectoryProperty

    /**
     * Path to the Maven executable.
     *
     * This should be different per OS:
     *
     * * Windows: `$mavenInstallDir/bin/mvn.cmd`
     * * Unix: `$mavenInstallDir/bin/mvn`
     */
    abstract val mvn: RegularFileProperty
}

val mavenCliSetupExtension =
    extensions.create("mavenCliSetup", MavenCliSetupExtension::class).apply {
        mavenVersion.convention(libs.versions.apacheMaven.core)
        mavenPluginToolsVersion.convention(libs.versions.apacheMaven.pluginTools)

        mavenBuildDir.convention(layout.buildDirectory.dir("maven"))
        mavenInstallDir.convention(layout.buildDirectory.dir("apache-maven"))

        val isWindowsProvider =
            providers.systemProperty("os.name").map { "win" in it.toLowerCase() }

        mvn.convention(
            providers.zip(mavenInstallDir, isWindowsProvider) { mavenInstallDir, isWindows ->
                mavenInstallDir.file(
                    when {
                        isWindows -> "bin/mvn.cmd"
                        else -> "bin/mvn"
                    }
                )
            }
        )
    }

val mavenBinary by configurations.registering {
    description = "used to download the Maven binary"
    isCanBeResolved = true
    isCanBeConsumed = false
    isVisible = false

    defaultDependencies {
        addLater(mavenCliSetupExtension.mavenVersion.map { mavenVersion ->
            project.dependencies.create(
                group = "org.apache.maven",
                name = "apache-maven",
                version = mavenVersion,
                classifier = "bin",
                ext = "zip"
            )
        })
    }
}

tasks.clean {
    delete(mavenCliSetupExtension.mavenBuildDir)
    delete(mavenCliSetupExtension.mavenInstallDir)
}

val installMavenBinary by tasks.registering(Sync::class) {
    val archives = serviceOf<ArchiveOperations>()
    from(
        mavenBinary.flatMap { conf ->
            @Suppress("UnstableApiUsage")
            val resolvedArtifacts = conf.incoming.artifacts.resolvedArtifacts

            resolvedArtifacts.map { artifacts ->
                artifacts.map { archives.zipTree(it.file) }
            }
        }
    ) {
        eachFile {
            // drop the first directory inside the zipped Maven bin (apache-maven-$version)
            relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
        }
        includeEmptyDirs = false
    }
    into(mavenCliSetupExtension.mavenInstallDir)
}
