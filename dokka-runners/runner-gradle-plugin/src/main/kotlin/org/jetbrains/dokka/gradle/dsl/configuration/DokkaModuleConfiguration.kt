/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.configuration

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl
import org.jetbrains.dokka.gradle.dsl.formats.DokkaFormatsContainer
import org.jetbrains.dokka.gradle.dsl.plugins.DokkaPluginsContainer

@DokkaGradlePluginDsl
public interface DokkaPerModuleConfiguration : DokkaModuleConfiguration {
    // glob/regex
    public val matching: Property<String>
}

@DokkaGradlePluginDsl
public interface DokkaModuleConfiguration : DokkaModuleBasedConfiguration {
    public val suppress: Property<Boolean>

    public val moduleName: Property<String>
    public val moduleVersion: Property<String>
}

@DokkaGradlePluginDsl
public interface DokkaModuleBasedConfiguration : DokkaSourceSetBasedConfiguration {
    public val suppressObviousFunctions: Property<Boolean>
    public val suppressInheritedMembers: Property<Boolean>

    public val perSourceSets: SetProperty<DokkaPerSourceSetConfiguration>
    public fun perSourceSet(configure: DokkaPerSourceSetConfiguration.() -> Unit)
    public fun perSourceSet(matching: String, configure: DokkaPerSourceSetConfiguration.() -> Unit)

    public val formats: DokkaFormatsContainer
    public fun formats(configure: DokkaFormatsContainer.() -> Unit)

    public val plugins: DokkaPluginsContainer
    public fun plugins(configure: DokkaPluginsContainer.() -> Unit)
}
