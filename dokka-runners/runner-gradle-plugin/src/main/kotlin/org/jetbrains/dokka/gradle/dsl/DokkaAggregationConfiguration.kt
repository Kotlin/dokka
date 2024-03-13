/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

public enum class DokkaAggregationLayout {
    NoCopy,
    CompactInParent
}

@DokkaGradlePluginDsl
public interface DokkaAggregationConfiguration {
    public val applyPluginToIncludedProjects: Property<Boolean>

    public val aggregationLayout: Property<DokkaAggregationLayout>

    public val includedProjects: SetProperty<String>

    public fun includeProjects(
        vararg patterns: String,
        configure: DokkaAggregationProjectsConfiguration.() -> Unit = {}
    ) {
    }

    public fun includeProjects(
        patterns: Iterable<String>,
        configure: DokkaAggregationProjectsConfiguration.() -> Unit = {}
    ) {
    }

    public fun includeAllProjects(configure: DokkaAggregationProjectsConfiguration.() -> Unit = {}) {}
    public fun includeSubprojects(configure: DokkaAggregationProjectsConfiguration.() -> Unit = {}) {}
}

@DokkaGradlePluginDsl
public interface DokkaAggregationProjectsConfiguration {
    public val excludedPatterns: SetProperty<String>

    public fun exclude(vararg patterns: String) {}
    public fun exclude(patterns: Iterable<String>) {}
}
