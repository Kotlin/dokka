/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.jetbrains.dokka.gradle.dsl.configuration.DokkaRootConfiguration

// TODO:
//  Action vs lambda with extension receiver
//  - Action works fine with Groovy
//  - lambda can have default parameters
//  path: Any - is bad, but useful - can be "../docs", can be file(""), can be provider from other task
//   may be it's possible to allow only just String, File, Provider<File>, RegularFile, etc
@DokkaGradlePluginDsl
public interface DokkaExtension : DokkaRootConfiguration {
// TODO: placement - per format, aggregation, etc
//   public val outputDirectory: DirectoryProperty

    public val generation: DokkaGenerationExtension
    public fun generation(configure: DokkaGenerationExtension.() -> Unit)

    public val aggregation: DokkaAggregationExtension
    public fun aggregation(configure: DokkaAggregationExtension.() -> Unit)
}
