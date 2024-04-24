/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

apply(from = "template.settings.gradle.kts")

pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "dokka-ui-test-project"
include(":jvm")
include(":kmp")
