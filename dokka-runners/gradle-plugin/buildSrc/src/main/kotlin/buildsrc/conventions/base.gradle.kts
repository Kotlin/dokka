/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package buildsrc.conventions

/**
 * A convention plugin that sets up common config and sensible defaults for all subprojects.
 */

plugins {
    base
}

if (project != rootProject) {
    project.version = rootProject.version
    project.group = rootProject.group
}

tasks.withType<AbstractArchiveTask>().configureEach {
    // https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<AbstractCopyTask>().configureEach {
    includeEmptyDirs = false
}
