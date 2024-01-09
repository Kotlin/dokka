/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.formats

import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl

// what is better: ExtensiblePolymorphicDomainObjectContainer vs ExtensionAware(ExtensionContainer)?
// same idea as with DokkaPluginsContainer
@DokkaGradlePluginDsl
public interface DokkaFormatsContainer : ExtensiblePolymorphicDomainObjectContainer<DokkaFormatConfiguration> {
    // calling this first time will automatically set `enabled=true`?
    public val html: DokkaHtmlFormatConfiguration // TODO: do we need such accessors here?
    public fun html(configure: DokkaHtmlFormatConfiguration.() -> Unit = {})
    public fun javadoc(configure: DokkaJavadocFormatConfiguration.() -> Unit = {})
    public fun gfm(configure: DokkaGfmFormatConfiguration.() -> Unit = {})
    public fun jekyll(configure: DokkaJekyllFormatConfiguration.() -> Unit = {})
}
