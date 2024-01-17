/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.provider.SetProperty
import org.jetbrains.dokka.gradle.dsl.configuration.DokkaModuleBasedConfiguration
import org.jetbrains.dokka.gradle.dsl.configuration.DokkaPerModuleConfiguration

@DokkaGradlePluginDsl
public interface DokkaSettingsExtension : DokkaBaseExecution, DokkaModuleBasedConfiguration {
    // TODO: some DSL to which projects to apply dokka
    public fun applyDokkaToKotlinProjects(enabled: Boolean = true)
    public fun applyDokkaToJavaProjects(enabled: Boolean = true)
    public fun applyDokkaToProjects(vararg projects: String)
    public fun applyDokkaToAllProjectsExcluding(vararg projects: String)

    // matching based on a Gradle project path?
    public val perModules: SetProperty<DokkaPerModuleConfiguration>
    public fun perModule(configure: DokkaPerModuleConfiguration.() -> Unit)
    public fun perModule(matching: String, configure: DokkaPerModuleConfiguration.() -> Unit)
}
