/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.aggregation

import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl

@DokkaGradlePluginDsl
public interface DokkaAggregation {
    // f.e. for included builds. maybe pick another name
    public fun include(dependencyNotation: Any)

    public fun includeProjects(
        vararg projects: String,
        configure: DokkaAggregationProjectsConfiguration.() -> Unit = {}
    )

    public fun includeProjects(
        projects: List<String>,
        configure: DokkaAggregationProjectsConfiguration.() -> Unit = {}
    )

    // TODO: how to make users always write `exclude`, without it, the line looks confusing
    public fun includeAllprojects(
        exclude: List<String> = emptyList(),
        configure: DokkaAggregationProjectsConfiguration.() -> Unit = {}
    )

    public fun includeSubprojects(
        exclude: List<String> = emptyList(),
        configure: DokkaAggregationProjectsConfiguration.() -> Unit = {}
    )
}
