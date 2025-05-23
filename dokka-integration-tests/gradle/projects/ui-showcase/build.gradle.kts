/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("uitest.dokka")
}

dependencies {
    dokka(project(":jvm"))
    dokka(project(":kmp"))
}
