/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

// settings.gradle.kts

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    plugin("org.jetbrains.dokka:versioning-plugin:1.9.10")
}
