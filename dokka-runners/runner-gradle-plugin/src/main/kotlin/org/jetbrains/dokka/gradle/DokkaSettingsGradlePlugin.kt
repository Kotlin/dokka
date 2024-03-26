/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.jetbrains.dokka.gradle.dsl.DokkaSettingsExtension
import org.jetbrains.dokka.gradle.internal.DefaultDokkaSettingsExtension

public abstract class DokkaSettingsGradlePlugin : Plugin<Settings> {
    override fun apply(target: Settings) {
        // just stubs
        target.extensions.create(
            DokkaSettingsExtension::class.java,
            "dokka",
            DefaultDokkaSettingsExtension::class.java,
            target
        )
    }
}
