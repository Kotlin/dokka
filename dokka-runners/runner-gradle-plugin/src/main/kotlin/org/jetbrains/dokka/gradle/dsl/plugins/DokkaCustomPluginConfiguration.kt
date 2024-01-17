/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.plugins

import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl
import org.jetbrains.dokka.gradle.dsl.properties.DokkaProperties

@DokkaGradlePluginDsl
public interface DokkaCustomPluginConfiguration : DokkaPluginConfiguration {
    public val pluginClassName: Property<String>

    // not really a good API - I think
    public val dependencies: Configuration
    public fun dependency(dependencyNotation: Any)

    public val properties: DokkaProperties
    public fun properties(block: DokkaProperties.() -> Unit)
}
