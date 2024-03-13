/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.Named

@DokkaGradlePluginDsl
public interface DokkaVariantBasedConfiguration : DokkaModuleBasedConfiguration {
    public val currentProject: DokkaCurrentProjectConfiguration
    public fun currentProject(configure: DokkaCurrentProjectConfiguration.() -> Unit) {}

    public val aggregation: DokkaAggregationConfiguration
    public fun aggregation(configure: DokkaAggregationConfiguration.() -> Unit) {}
}

@DokkaGradlePluginExperimentalApi
@DokkaGradlePluginDelicateApi
@DokkaGradlePluginDsl
public interface DokkaVariant : Named, DokkaVariantBasedConfiguration
