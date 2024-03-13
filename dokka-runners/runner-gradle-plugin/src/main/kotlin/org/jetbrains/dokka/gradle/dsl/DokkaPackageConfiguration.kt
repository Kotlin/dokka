/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

@DokkaGradlePluginDsl
public interface DokkaPerPackageConfiguration : DokkaPackageConfiguration {
    public val pattern: Property<String>
}

@DokkaGradlePluginDsl
public interface DokkaPackageConfiguration : DokkaPackageBasedConfiguration {
    public val suppress: Property<Boolean>
}

@DokkaGradlePluginDsl
public interface DokkaPackageBasedConfiguration: DokkaExecutionConfiguration {
    // only public by default
    public val documentedVisibilities: SetProperty<DokkaDeclarationVisibility>
    public val warnOnUndocumented: Property<Boolean>
    public val includeDeprecated: Property<Boolean>
}

