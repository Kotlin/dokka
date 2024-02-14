/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

apply(from = "template.settings.gradle.kts")
rootProject.name = "it-multimodule-versioning-0"
include(":first")
include(":second")
