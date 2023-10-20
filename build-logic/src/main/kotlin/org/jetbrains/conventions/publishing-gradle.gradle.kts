/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.conventions

plugins {
    id("org.jetbrains.conventions.publishing-base")
    id("com.gradle.plugin-publish")
}

gradlePlugin {
    website.set("https://kotl.in/dokka")
    vcsUrl.set("https://github.com/kotlin/dokka.git")
}
