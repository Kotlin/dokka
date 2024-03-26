/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer

public interface DokkaSettingsExtension : DokkaSettingsVariantConfiguration {
    @DokkaGradlePluginExperimentalApi
    @DokkaGradlePluginDelicateApi
    public val variants: NamedDomainObjectContainer<DokkaSettingsVariant>
}

@DokkaGradlePluginExperimentalApi
@DokkaGradlePluginDelicateApi
@DokkaGradlePluginDsl
public interface DokkaSettingsVariant : Named, DokkaSettingsVariantConfiguration

@DokkaGradlePluginDsl
public interface DokkaSettingsVariantConfiguration : DokkaModuleBasedConfiguration
