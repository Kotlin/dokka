/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
}

// dummy project to satisfy the Gradle soft-requirement that the subproject name matches the artifact name

dependencies {
    api(projects.core)
}
