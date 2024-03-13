/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

@DokkaGradlePluginDsl
public interface DokkaSourceLinkConfiguration {
    public val localDirectory: DirectoryProperty
    public val remoteUrl: Property<String>
    public val remoteLineSuffix: Property<String>
}
