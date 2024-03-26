/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.PluginAware
import org.gradle.kotlin.dsl.apply

public abstract class DokkaGradlePlugin : Plugin<PluginAware> {
    override fun apply(target: PluginAware): Unit = when (target) {
        is Project -> target.apply<DokkaProjectGradlePlugin>()
        is Settings -> target.apply<DokkaSettingsGradlePlugin>()
        else -> error("not supported")
    }
}
