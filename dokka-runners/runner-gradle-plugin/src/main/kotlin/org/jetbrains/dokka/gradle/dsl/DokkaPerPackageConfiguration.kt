/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

@DokkaGradlePluginDsl
public interface DokkaPerPackageConfiguration : DokkaPackageBasedConfiguration {
    public val matchingRegex: Property<String>
    public val suppress: Property<Boolean>

    public val packageDocumentation: ConfigurableFileCollection

    // for simple cases
    // `path: Any` resolved as project.file(file)
    public fun packageDocumentation(text: String)
    public fun packageDocumentationFrom(path: Any)
}

@DokkaGradlePluginDsl
public interface DokkaPackageBasedConfiguration {
    // only public by default
    public val documentedVisibilities: SetProperty<DokkaDeclarationVisibility>
    public val reportUndocumented: Property<Boolean> // or warnOnUndocumented
    public val includeDeprecated: Property<Boolean> // old skipDeprecated
}
