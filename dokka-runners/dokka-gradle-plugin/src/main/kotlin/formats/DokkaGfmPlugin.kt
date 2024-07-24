/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.formats

import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi

abstract class DokkaGfmPlugin
@DokkaInternalApi
constructor() : DokkaFormatPlugin(formatName = "gfm") {
    override fun DokkatooFormatPluginContext.configure() {
        project.dependencies {
            dokkaPlugin(dokka("gfm-plugin"))
        }
    }
}
