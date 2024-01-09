/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.plugins

import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl

@DokkaGradlePluginDsl
public interface DokkaPluginConfiguration : Named {
    public val enabled: Property<Boolean>
}
