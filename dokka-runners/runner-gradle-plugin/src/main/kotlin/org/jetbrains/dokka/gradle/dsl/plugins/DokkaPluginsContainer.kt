/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.plugins

import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl

// same idea as with DokkaFormatsContainer
@DokkaGradlePluginDsl
public interface DokkaPluginsContainer : ExtensiblePolymorphicDomainObjectContainer<DokkaPluginConfiguration> {
    public fun mathjax(configure: DokkaMathjaxPluginConfiguration.() -> Unit)
    public fun kotlinAsJava(configure: DokkaKotlinAsJavaPluginConfiguration.() -> Unit)
    public fun versioning(configure: DokkaVersioningPluginConfiguration.() -> Unit)

    // TODO: naming
    public fun custom(configure: DokkaCustomPluginConfiguration.() -> Unit)
    public fun custom(
        fqPluginName: String,
        dependencyNotation: Any,
        configure: DokkaCustomPluginConfiguration.() -> Unit = {}
    )
}
