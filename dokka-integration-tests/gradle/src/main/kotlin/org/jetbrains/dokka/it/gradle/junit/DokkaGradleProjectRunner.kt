/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle.junit

import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.dokka.gradle.utils.ProjectDirectoryScope
import java.nio.file.Path

class DokkaGradleProjectRunner(
    override val projectDir: Path,
    val runner: GradleRunner,
) : ProjectDirectoryScope
