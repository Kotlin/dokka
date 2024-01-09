/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.plugins

import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl

@DokkaGradlePluginDsl
public interface DokkaCustomPluginConfiguration : DokkaPluginConfiguration {
    public val pluginClassName: Property<String>

    // TODO: add more type-safety (as in dokkatoo)
    public val pluginProperties: MapProperty<String, String>
    public fun pluginProperty(name: String, value: Boolean)
    public fun pluginProperty(name: String, value: Int)
    public fun pluginProperty(name: String, value: String)

    public val dependencies: Configuration
    public fun dependency(dependencyNotation: Any)
}
