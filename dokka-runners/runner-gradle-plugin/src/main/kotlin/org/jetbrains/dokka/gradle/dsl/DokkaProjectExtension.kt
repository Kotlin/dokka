/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

@DokkaGradlePluginDsl
public interface DokkaProjectExtension : DokkaVariantBasedConfiguration {
    @DokkaGradlePluginExperimentalApi
    @DokkaGradlePluginDelicateApi
    public val variants: NamedDomainObjectContainer<DokkaVariant>
}

@DokkaGradlePluginDsl
public interface DokkaCurrentProjectConfiguration : DokkaModuleBasedConfiguration {
    public val moduleName: Property<String>
    public val moduleVersion: Property<String>
    public val outputDirectory: DirectoryProperty

    @DokkaGradlePluginDelicateApi
    public val sourceSets: NamedDomainObjectContainer<DokkaSourceSet>
}
