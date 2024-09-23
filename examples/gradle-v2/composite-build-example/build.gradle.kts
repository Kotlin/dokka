/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    base
}

tasks.build {
    dependsOn(gradle.includedBuild("docs").task(":dokkaGenerate"))
}

group = "foo.example.composite.builds"
version = "1.0.1"


tasks.clean {
    dependsOn(
        gradle.includedBuild("docs").task(":clean"),
        gradle.includedBuild("module-kakapo").task(":clean"),
        gradle.includedBuild("module-kea").task(":clean"),
    )
}
