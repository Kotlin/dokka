/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.plugins

import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl

// same idea as with DokkaFormatsContainer
@DokkaGradlePluginDsl
public interface DokkaPluginsContainer : ExtensiblePolymorphicDomainObjectContainer<DokkaPluginConfiguration> {
    public fun mathjax(enabled: Boolean = true, configure: DokkaMathjaxPluginConfiguration.() -> Unit)
    public fun kotlinAsJava(enabled: Boolean = true, configure: DokkaKotlinAsJavaPluginConfiguration.() -> Unit)
    public fun versioning(enabled: Boolean = true, configure: DokkaVersioningPluginConfiguration.() -> Unit)

    // TODO: naming
    public fun custom(configure: DokkaCustomPluginConfiguration.() -> Unit)
    public fun custom(
        className: String,
        dependencyNotation: Any,
        configure: DokkaCustomPluginConfiguration.() -> Unit = {}
    )
}
