/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

rootProject.name = "it-multimodule-1"

apply(from = "./template.settings.gradle.kts")

include(":first")
include(":second")
