/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.configuration

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.jetbrains.dokka.gradle.dsl.DokkaDeclarationVisibility
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl

@DokkaGradlePluginDsl
public interface DokkaPerPackageConfiguration : DokkaPackageBasedConfiguration {
    // glob/regex
    public val matching: Property<String>
    public val suppress: Property<Boolean>

    // for simple cases `packageDocumentation(...)`
    // `path: Any` resolved as project.file(file)
    public val packageDocumentation: ConfigurableFileCollection
    public fun packageDocumentation(text: String)
    public fun packageDocumentationFrom(path: Any)
}

// inherited by sourceSet->module->project or for perPackage
@DokkaGradlePluginDsl
public interface DokkaPackageBasedConfiguration {
    // only public by default
    public val documentedVisibilities: SetProperty<DokkaDeclarationVisibility>
    public val warnOnUndocumented: Property<Boolean> // or reportUndocumented
    public val includeDeprecated: Property<Boolean> // old skipDeprecated
}
