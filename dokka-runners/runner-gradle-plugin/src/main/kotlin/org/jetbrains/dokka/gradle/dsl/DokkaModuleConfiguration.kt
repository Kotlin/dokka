/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

// will be shared with included projects
@DokkaGradlePluginDsl
public interface DokkaModuleBasedConfiguration : DokkaSourceSetBasedConfiguration {
    public val dokkaEngineVersion: Property<String>
    public val offlineMode: Property<Boolean>
    public val warningsAsErrors: Property<Boolean>

    public val suppressObviousFunctions: Property<Boolean>
    public val suppressInheritedMembers: Property<Boolean>

    public val html: DokkaHtmlConfiguration
    public fun html(configure: DokkaHtmlConfiguration.() -> Unit) {}

    @DokkaGradlePluginExperimentalApi
    public val versioning: DokkaVersioningConfiguration

    // calling this will enable versioning
    @DokkaGradlePluginExperimentalApi
    public fun versioning(enabled: Boolean = true, configure: DokkaVersioningConfiguration.() -> Unit) {
    }

    public val perSourceSets: SetProperty<DokkaPerSourceSetConfiguration>
    public fun perSourceSet(configure: DokkaPerSourceSetConfiguration.() -> Unit) {}
    public fun perSourceSet(pattern: String, configure: DokkaPerSourceSetConfiguration.() -> Unit) {}

    public val perPackages: SetProperty<DokkaPerPackageConfiguration>
    public fun perPackage(configure: DokkaPerPackageConfiguration.() -> Unit) {}
    public fun perPackage(pattern: String, configure: DokkaPerPackageConfiguration.() -> Unit) {}

    public fun plugins(vararg dependencyNotation: Any) {}
    public fun pluginConfiguration(configure: DokkaPluginConfiguration.() -> Unit) {}
    public fun pluginConfiguration(pluginClassName: String, configure: DokkaPluginConfiguration.() -> Unit) {}

    public fun plugin(
        dependencyNotation: Any,
        configure: DokkaPluginConfiguration.() -> Unit = {}
    ) {
    }

    public fun plugin(
        dependencyNotation: Any,
        pluginClassName: String,
        configure: DokkaPluginConfiguration.() -> Unit
    ) {
    }
}
