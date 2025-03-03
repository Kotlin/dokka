/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    pluginsConfiguration.versioning {
        olderVersionsDir.set(rootDir.resolve("previousDocVersions"))
    }
}

dependencies {
    // No version is necessary, Dokka will add it automatically
    dokkaHtmlPlugin("org.jetbrains.dokka:versioning-plugin")
}
