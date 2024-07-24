/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.formats

import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.dokka.gradle.internal.DokkatooInternalApi

abstract class DokkatooJavadocPlugin
@DokkatooInternalApi
constructor() : DokkatooFormatPlugin(formatName = "javadoc") {
    override fun DokkatooFormatPluginContext.configure() {
        project.dependencies {
            dokkaPlugin(dokka("javadoc-plugin"))
        }
    }
}
