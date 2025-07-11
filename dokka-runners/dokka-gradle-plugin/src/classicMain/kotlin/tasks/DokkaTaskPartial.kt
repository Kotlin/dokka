/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.dokka.gradle

import org.gradle.api.tasks.CacheableTask
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.build

@CacheableTask
@Deprecated(DOKKA_V1_DEPRECATION_MESSAGE)
abstract class DokkaTaskPartial : @Suppress("DEPRECATION") AbstractDokkaLeafTask() {

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        return DokkaConfigurationImpl(
            moduleName = moduleName.get(),
            moduleVersion = moduleVersion.orNull,
            outputDir = outputDirectory.asFile.get(),
            cacheRoot = cacheRoot.asFile.orNull,
            offlineMode = offlineMode.get(),
            failOnWarning = failOnWarning.get(),
            sourceSets = unsuppressedSourceSets.build(),
            pluginsConfiguration = buildPluginsConfiguration(),
            pluginsClasspath = plugins.resolve().toList(),
            delayTemplateSubstitution = true,
            suppressObviousFunctions = suppressObviousFunctions.get(),
            suppressInheritedMembers = suppressInheritedMembers.get(),
        )
    }
}
