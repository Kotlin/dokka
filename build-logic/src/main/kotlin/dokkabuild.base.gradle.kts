/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import dokkabuild.DokkaBuildProperties
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP

/**
 * A convention plugin that sets up common config and sensible defaults for all subprojects.
 *
 * It provides the [DokkaBuildProperties] extension, for accessing common build properties.
 */

plugins {
    base
}

extensions.create<DokkaBuildProperties>(DokkaBuildProperties.EXTENSION_NAME)

tasks.withType<AbstractArchiveTask>().configureEach {
    // https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val integrationTestPreparation by tasks.registering {
    description =
        "lifecycle task for preparing the project for integration tests (for example, publishing to the test Maven repo)"
    group = VERIFICATION_GROUP
}


//region jvmArgs logging
// jvmArgs seem to change on CI, which causes Build Cache misses, hampering build performance
// The easiest way to investigate them is to log them on CI.
if (dokkaBuild.isCI.get()) {
    tasks
        .matching { it is JavaForkOptions }
        .configureEach {
            val task = this as? JavaForkOptions? ?: return@configureEach
            doFirst("log jvmArgs") {
                logger.lifecycle("[$path] jvmArgs: ${task.jvmArgs}")
            }
        }
}
//endregion
