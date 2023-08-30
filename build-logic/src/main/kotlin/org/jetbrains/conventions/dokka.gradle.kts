/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.conventions

import org.gradle.kotlin.dsl.invoke
import org.jetbrains.isLocalPublication

plugins {
    id("org.jetbrains.dokka")
}

tasks.dokkaHtml {
    onlyIf { !isLocalPublication }
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
}
