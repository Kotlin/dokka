/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.dsl.DokkaDeclarationVisibility

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    documentedVisibilities.add(DokkaDeclarationVisibility.PROTECTED)
    sourceLink("https://github.com/kotlin/dokka/tree/master")

    aggregation {
        excludeProjects("common-utils")
    }
}

subprojects {
    plugins.apply("org.jetbrains.dokka")
}
