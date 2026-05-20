/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle.junit

import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.dokka.gradle.utils.ProjectDirectoryScope
import org.jetbrains.dokka.gradle.utils.file
import java.nio.file.Path
import kotlin.io.path.deleteRecursively

/**
 * Details of a Gradle project, with a [GradleRunner], for running tests.
 *
 * New instances are created by [DokkaGradlePluginTestExtension], which will also populate [projectDir]
 * with a freshly created project.
 */
class DokkaGradleProjectRunner(
    override val projectDir: Path,
    val runner: GradleRunner,
) : ProjectDirectoryScope {

    /**
     * Delete all configuration cache caches and reports.
     */
    fun deleteConfigurationCacheData() {
        file(".gradle/configuration-cache").deleteRecursively()
        file("build/reports/configuration-cache").deleteRecursively()
    }
}
