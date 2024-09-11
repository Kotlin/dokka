/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.formats

import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi

/**
 * Gradle plugin that configures Dokka Javadoc output format
 */
abstract class DokkaJavadocPlugin
@DokkaInternalApi
constructor() : DokkaFormatPlugin(formatName = "javadoc") {
    override fun DokkaFormatPluginContext.configure() {
        project.dependencies {
            dokkaPlugin(dokka("javadoc-plugin"))
        }
    }
}
