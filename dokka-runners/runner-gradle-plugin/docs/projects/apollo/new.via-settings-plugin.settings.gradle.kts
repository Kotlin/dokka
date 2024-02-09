/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

// settings.gradle.kts

plugins {
    id("org.jetbrains.dokka")
}

// TODO[whyoleg]: is it possible to share some relative paths inside related to projects
// f.e. `includedDocumentation.from("README.md")` should mean, that `README` file should be in each project
// but still it should be possible to include paths like `rootDir.resolve(...)` which should be the same
dokka {
    html {
        customStyleSheets.from(
            listOf("style.css", "prism.css", "logo-styles.css").map { rootDir.resolve("dokka/$it") }
        )
        customAssets.from(
            listOf("apollo.svg").map { rootDir.resolve("dokka/$it") }
        )
    }
    includedDocumentation.from("README.md")
}
