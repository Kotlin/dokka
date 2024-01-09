/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.aggregation

import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl

// TODO: multi-module vs collector(merged)
//  multi-module fileLayout, additional includes?
@DokkaGradlePluginDsl
public interface DokkaMultiModuleAggregation : DokkaAggregation {
    public val fileLayout: Property<DokkaMultiModuleFileLayout>
}
