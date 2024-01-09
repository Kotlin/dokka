/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.aggregation

import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl
import org.jetbrains.dokka.gradle.dsl.GradleIsolatedProjectsRestrictions

@DokkaGradlePluginDsl
public interface DokkaAggregationProjectsConfiguration {
    // TODO: which specific properties can be inherited?
    // false by default
    @GradleIsolatedProjectsRestrictions("https://github.com/gradle/gradle/issues/25179")
    public val inheritConfiguration: Property<Boolean>

    // false by default
    @GradleIsolatedProjectsRestrictions("https://github.com/gradle/gradle/issues/22514")
    public val applyDokkaPlugin: Property<Boolean>
}
