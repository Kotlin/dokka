/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("jvm")
}

kotlin {
    explicitApi()
}

dependencies {
    api(projects.kotlinDocumentation.kotlinDocumentationModel)
}
