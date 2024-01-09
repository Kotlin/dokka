/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property

@DokkaGradlePluginDsl
public interface DokkaPerModuleConfiguration : DokkaModuleBasedConfiguration<DokkaSourceSetConfiguration> {
    public val matchingRegex: Property<String>
    public val suppress: Property<Boolean>
}

@DokkaGradlePluginDsl
public interface DokkaModuleBasedConfiguration<SSC : DokkaSourceSetConfiguration> : DokkaSourceSetBasedConfiguration {
    public val moduleName: Property<String>
    public val moduleVersion: Property<String>

    public val suppressObviousFunctions: Property<Boolean>
    public val suppressInheritedMembers: Property<Boolean>

    public val sourceSets: NamedDomainObjectContainer<SSC>
    public fun sourceSets(configure: NamedDomainObjectContainer<SSC>.() -> Unit)
}
