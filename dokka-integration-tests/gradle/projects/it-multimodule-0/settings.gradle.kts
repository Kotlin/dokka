/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

rootProject.name = "it-multimodule-0"

apply(from = "./template.settings.gradle.kts")

include(":moduleA")
include(":moduleA:moduleB")
include(":moduleA:moduleC")
include(":moduleA:moduleD")
