/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.jetbrains.dokka.gradle.dsl.aggregation.DokkaCollectedAggregation
import org.jetbrains.dokka.gradle.dsl.aggregation.DokkaMultiModuleAggregation
import org.jetbrains.dokka.gradle.dsl.formats.DokkaFormatsContainer
import org.jetbrains.dokka.gradle.dsl.plugins.DokkaPluginsContainer

// TODO:
//  Action vs lambda with extension receiver
//  - Action works fine with Groovy
//  - lambda can have default parameters
//  path: Any - is bad, but useful - can be "../docs", can be file(""), can be provider from other task
//   may be it's possible to allow only just String, File, Provider<File>, RegularFile, etc
@DokkaGradlePluginDsl
public interface DokkaExtension : DokkaModuleConfiguration<DokkaSourceSet> {
    // TODO: placement - per format, aggregation, etc
    public val outputDirectory: DirectoryProperty
}

// TODO?
@DokkaGradlePluginDsl
public interface DokkaSettingsExtension : DokkaModuleConfiguration<DokkaSourceSetConfiguration> {

    // TODO: placement - per format, aggregation, etc
    public val outputDirectory: DirectoryProperty

    public val perModuleConfigurations: SetProperty<DokkaPerModuleConfiguration>
    public fun perModuleConfiguration(configure: DokkaPerModuleConfiguration.() -> Unit)
    public fun perModuleConfiguration(matchingRegex: String, configure: DokkaPerModuleConfiguration.() -> Unit)
}

@DokkaGradlePluginDsl
public interface DokkaModuleConfiguration<SSC : DokkaSourceSetConfiguration> : DokkaModuleBasedConfiguration<SSC> {
    // possibility to use DGP(Dokka Gradle Plugin) version=KGP version, but newer/older/patched analysis
    // default to the DGP version
    public val dokkaAnalysisVersion: Property<String>

    public val offlineMode: Property<Boolean>
    public val failOnWarning: Property<Boolean>

    public val formats: DokkaFormatsContainer
    public fun formats(configure: DokkaFormatsContainer.() -> Unit)

    public val plugins: DokkaPluginsContainer
    public fun plugins(configure: DokkaPluginsContainer.() -> Unit)

    // TODO: multiModule vs aggregation.multiModule vs ...
    public val multiModule: DokkaMultiModuleAggregation
    public fun multiModule(configure: DokkaMultiModuleAggregation.() -> Unit)

    public val collected: DokkaCollectedAggregation
    public fun collected(configure: DokkaCollectedAggregation.() -> Unit)
}
