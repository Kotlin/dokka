/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.jetbrains.dokka.gradle.dsl.DokkaProjectExtension
import org.jetbrains.dokka.gradle.internal.DefaultDokkaProjectExtension
import org.jetbrains.dokka.gradle.tasks.DokkaBuildTask

public abstract class DokkaGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // just stubs
        target.extensions.create(
            DokkaProjectExtension::class.java,
            "dokka",
            DefaultDokkaProjectExtension::class.java,
            target
        )
        target.tasks.register<DokkaBuildTask>("dokkaBuild")
    }
}
