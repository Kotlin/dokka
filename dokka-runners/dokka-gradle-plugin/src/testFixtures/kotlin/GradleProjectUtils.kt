/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils

import org.gradle.api.Project
import org.jetbrains.dokka.gradle.internal.PluginFeaturesService.Companion.V2_PLUGIN_ENABLED_FLAG
import org.jetbrains.dokka.gradle.internal.PluginFeaturesService.Companion.V2_PLUGIN_ENABLED_QUIET_FLAG

fun Project.enableV2Plugin(
    suppressV2Message: Boolean = true
): Project {
    extensions.extraProperties.set(V2_PLUGIN_ENABLED_FLAG, true)
    extensions.extraProperties.set(V2_PLUGIN_ENABLED_QUIET_FLAG, suppressV2Message)
    return this
}
