/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.formats

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl

@DokkaGradlePluginDsl
public interface DokkaHtmlFormatConfiguration : DokkaFormatConfiguration {
    public val customAssets: ConfigurableFileCollection
    public val customStyleSheets: ConfigurableFileCollection
    public val templatesDirectory: DirectoryProperty

    public val separateInheritedMembers: Property<Boolean>
    public val mergeImplicitExpectActualDeclarations: Property<Boolean>
    public val footerMessage: Property<String>
    public val homepageLink: Property<String>
}
