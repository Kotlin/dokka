/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    // TODO: File bug report for gradle: :moduleA:moduleB:dokkaHtml is missing kotlin gradle plugin from
    //  the runtime classpath during execution without this plugin in the parent project
    kotlin("jvm")
    id("org.jetbrains.dokka")
}
