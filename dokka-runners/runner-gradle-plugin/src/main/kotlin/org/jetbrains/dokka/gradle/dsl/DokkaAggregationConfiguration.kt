/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.provider.Property

public enum class DokkaAggregationLayout {
    NoCopy,
    CompactInParent
}

@DokkaGradlePluginDsl
public interface DokkaAggregationConfiguration {
    public val aggregationLayout: Property<DokkaAggregationLayout>

    public fun includeProjects(vararg patterns: String) {}
    public fun includeProjects(patterns: Iterable<String>) {}

    public fun excludeProjects(vararg patterns: String) {}
    public fun excludeProjects(patterns: Iterable<String>) {}

    // shortcuts
    public fun includeAllProjects() {}
    public fun includeSubprojects() {}
}
