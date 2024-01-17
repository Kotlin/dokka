/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.configuration

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl

@DokkaGradlePluginDsl
public interface DokkaSourceLinkConfiguration {
    public val localDirectory: DirectoryProperty

    // TODO: `null` overrides any other sourceLink to suppress generation of source buttons
    //  f.e if sources are generated or in private repository, or correct `localDirectory`
    public val remoteUrl: Property<String?>
    public val remoteLineSuffix: Property<String>
}
