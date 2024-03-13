/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

pluginManagement {
    // include `runner-gradle-plugin`
    includeBuild("../../.")
}

rootProject.name = "runner-gradle-plugin-playground-coroutines-like"

// module where aggregation happens
include("kdoc")
