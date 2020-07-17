package org.jetbrains

import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import java.io.File

@Suppress("LeakingThis")
open class SetupMaven : Sync() {
    @get:Input
    var mavenVersion = "3.5.0"

    @get:Input
    var mavenPluginToolsVersion = "3.5.2"

    @get:Input
    var aetherVersion = "1.1.0"

    @get:Internal
    val mavenBuildDir = "${project.buildDir}/maven"

    @get:Internal
    val mavenBinDir = "${project.buildDir}/maven-bin"

    @get:Internal
    val mvn = File(mavenBinDir, "apache-maven-$mavenVersion/bin/mvn")

    private val mavenBinaryConfiguration: Configuration by project.configurations.creating {
        project.dependencies {
            this@creating.invoke(
                group = "org.apache.maven",
                name = "apache-maven",
                version = mavenVersion,
                classifier = "bin", ext = "zip"
            )
        }
    }

    init {
        from(mavenBinaryConfiguration.map { file -> project.zipTree(file) })
        into(mavenBinDir)
    }

}
