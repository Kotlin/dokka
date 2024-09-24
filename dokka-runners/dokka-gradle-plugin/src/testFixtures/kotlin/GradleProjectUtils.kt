/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils

import org.gradle.api.Project

fun Project.enableV2Plugin(
    v2MigrationHelpers: Boolean = true,
    suppressV2Message: Boolean = true
): Project {
    extensions.extraProperties.set(
        "org.jetbrains.dokka.experimental.gradle.pluginMode",
        if (v2MigrationHelpers) "V2EnabledWithHelpers" else "V2Enabled",
    )
    extensions.extraProperties.set("org.jetbrains.dokka.experimental.gradle.pluginMode.noWarn", suppressV2Message)
    return this
}
