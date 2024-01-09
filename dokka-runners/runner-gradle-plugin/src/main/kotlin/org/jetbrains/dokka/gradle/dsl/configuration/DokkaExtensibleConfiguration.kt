/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.configuration

import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl
import org.jetbrains.dokka.gradle.dsl.formats.DokkaFormatsContainer
import org.jetbrains.dokka.gradle.dsl.plugins.DokkaPluginsContainer

// TODO: better naming
@DokkaGradlePluginDsl
public interface DokkaExtensibleConfiguration {
    public val formats: DokkaFormatsContainer
    public fun formats(configure: DokkaFormatsContainer.() -> Unit)

    public val plugins: DokkaPluginsContainer
    public fun plugins(configure: DokkaPluginsContainer.() -> Unit)
}
