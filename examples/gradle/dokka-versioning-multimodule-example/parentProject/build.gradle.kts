/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.versioning.VersioningPlugin
import org.jetbrains.dokka.versioning.VersioningConfiguration

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:versioning-plugin:1.9.20")
    }

    repositories {
        mavenCentral()
    }
}

val currentVersion = "1.0"
val previousVersionsDirectory = project.rootProject.projectDir.resolve("previousDocVersions").invariantSeparatorsPath

// Main configuration for the versioning plugin. It will generate documentation for
// the current version of the application, and look for previous versions of docs
// in the directory defined in previousVersionsDirectory, allowing it to create
// the version navigation dropdown menu.
tasks.dokkaHtmlMultiModule {
    pluginConfiguration<VersioningPlugin, VersioningConfiguration> {
        version = currentVersion
        olderVersionsDir = file(previousVersionsDirectory)
    }
}
