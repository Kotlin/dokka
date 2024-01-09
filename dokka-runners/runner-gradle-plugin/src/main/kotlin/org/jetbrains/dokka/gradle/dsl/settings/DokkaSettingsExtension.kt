/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.settings

import org.gradle.api.provider.SetProperty
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl
import org.jetbrains.dokka.gradle.dsl.configuration.DokkaPerModuleConfiguration
import org.jetbrains.dokka.gradle.dsl.configuration.DokkaRootConfiguration

@DokkaGradlePluginDsl
public interface DokkaSettingsExtension : DokkaRootConfiguration {
    // TODO: some DSL to which projects to apply dokka
    public fun applyDokkaToKotlinProjects()
    public fun applyDokkaToJavaProjects()
    public fun applyDokkaToProjects(vararg projects: String)
    public fun applyDokkaToAllProjectsExcluding(vararg projects: String)

    public val aggregation: DokkaSettingsAggregationExtension
    public fun aggregation(configure: DokkaSettingsAggregationExtension.() -> Unit)

    // matching based on a Gradle project path?
    public val perModules: SetProperty<DokkaPerModuleConfiguration>
    public fun perModule(configure: DokkaPerModuleConfiguration.() -> Unit)
    public fun perModule(matching: String, configure: DokkaPerModuleConfiguration.() -> Unit)
}
