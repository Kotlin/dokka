/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.utils

import org.gradle.api.Project

fun Project.enableDokkatoo() {
    extensions.extraProperties.set("DokkaGradlePluginMode", "dokkatoo")
}
