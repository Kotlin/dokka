/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.formats

import org.gradle.api.Named
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl
import org.jetbrains.dokka.gradle.dsl.plugins.DokkaPluginsContainer

@DokkaGradlePluginDsl
public interface DokkaFormatConfiguration : Named {
    // only html is enabled by default
    public val enabled: Property<Boolean>

    public val outputDirectory: DirectoryProperty

    // not really sure if it's needed
    public val plugins: DokkaPluginsContainer
    public fun plugins(configure: DokkaPluginsContainer.() -> Unit)
}