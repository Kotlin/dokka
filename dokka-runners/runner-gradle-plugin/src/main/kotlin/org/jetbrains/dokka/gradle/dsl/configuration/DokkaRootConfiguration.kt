/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.configuration

import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl

@DokkaGradlePluginDsl
public interface DokkaRootConfiguration : DokkaModuleBasedConfiguration {
    // possibility to use DGP(Dokka Gradle Plugin) version=KGP version, but newer/older/patched analysis
    // default to the DGP version
    // dokkaEngineVersion or dokkaAnalysisVersion
    public val dokkaEngineVersion: Property<String>

    public val offlineMode: Property<Boolean>
    public val failOnWarning: Property<Boolean>
}
