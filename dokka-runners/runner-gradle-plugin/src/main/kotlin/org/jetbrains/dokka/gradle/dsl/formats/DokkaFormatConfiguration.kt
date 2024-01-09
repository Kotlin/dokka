/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.formats

import org.gradle.api.Named
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl

@DokkaGradlePluginDsl
public interface DokkaFormatConfiguration : Named {
    // only html is enabled by default
    public val enabled: Property<Boolean>

    public val outputDirectory: DirectoryProperty

//    public fun generateJar(classifier: String?)
//    public fun generateJavadocJar()
}

// html format
// dokkaGenerateHtmlJar - no classifier
// dokkaGenerateHtmlJavadocJar - `javadoc` classifier

// javadoc format
// dokkaGenerateJavadocJar - no classifier
// dokkaGenerateJavadocJavadocJar - `javadoc` classifier
