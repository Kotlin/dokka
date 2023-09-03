/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

apply(from = "../template.settings.gradle.kts")
rootProject.name = "it-multimodule-0"
include(":moduleA")
include(":moduleA:moduleB")
include(":moduleA:moduleC")
