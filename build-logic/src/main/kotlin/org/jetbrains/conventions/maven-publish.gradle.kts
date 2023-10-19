/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.conventions

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin

plugins {
    id("org.jetbrains.conventions.base")
    `maven-publish`
    signing
}

plugins.withType<ShadowPlugin>().configureEach {
    // manually disable publication of Shadow elements https://github.com/johnrengelman/shadow/issues/651#issue-839148311
    // This is done to preserve compatibility and have the same behaviour as previous versions of Dokka.
    // For more details, see https://github.com/Kotlin/dokka/pull/2704#issuecomment-1499517930
    val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) { skip() }
}
