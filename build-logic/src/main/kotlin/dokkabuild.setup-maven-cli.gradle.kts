/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.tasks.MvnExec
import dokkabuild.utils.declarable
import dokkabuild.utils.resolvable
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

    companion object {
        const val MAVEN_PLUGIN_GROUP = "maven plugin"
    }
}

val mavenCliSetupExtension =
    extensions.create("mavenCliSetup", MavenCliSetupExtension::class).apply {
        mavenVersion.convention(libs.versions.apacheMaven.core)
        mavenPluginToolsVersion.convention(libs.versions.apacheMaven.pluginTools)

        mavenInstallDir.convention(layout.buildDirectory.dir("apache-maven"))

        val isWindowsProvider =
            providers.systemProperty("os.name").map { "win" in it.lowercase() }

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

val mavenBinary: Configuration by configurations.creating {
    description = "Apache Maven executable dependency"
    declarable()

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

val mavenBinaryResolver: Configuration by configurations.creating {
    description = "Resolves the Maven executable dependency"
    resolvable()
    extendsFrom(mavenBinary)
}

tasks.clean {
    delete(mavenCliSetupExtension.mavenInstallDir)
}

val installMavenBinary by tasks.registering {
    val archives = serviceOf<ArchiveOperations>()
    val fs = serviceOf<FileSystemOperations>()

    val mavenBinary = mavenBinaryResolver.incoming.files
    inputs.files(mavenBinary)
        .withPropertyName("mavenBinary")
        .withNormalizer(ClasspathNormalizer::class)

    val outputDir = mavenCliSetupExtension.mavenInstallDir
    outputs.dir(outputDir).withPropertyName("outputDir")

    doLast {
        val unpackedMavenBinary = mavenBinary.flatMap { artifact ->
            archives.zipTree(artifact)
        }

        fs.sync {
            from(unpackedMavenBinary) {
                eachFile {
                    // drop the first directory inside the zipped Maven bin (apache-maven-$version)
                    relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
                }
            }
            includeEmptyDirs = false
            into(outputDir)
        }
    }
}

tasks.withType<MvnExec>().configureEach {
    group = MavenCliSetupExtension.MAVEN_PLUGIN_GROUP
    dependsOn(installMavenBinary)
    mvnCli.convention(mavenCliSetupExtension.mvn)
    workDirectory.convention(layout.dir(provider { temporaryDir }))
    showErrors.convention(true)
    batchMode.convention(true)
    settingsXml.convention(layout.projectDirectory.file("settings.xml"))
}
