/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Provider
import org.jetbrains.dokka.gradle.dsl.configuration.DokkaSourceSetConfiguration
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

@DokkaGradlePluginDsl
public interface DokkaSourceSet : DokkaSourceSetConfiguration, Named {
    // TODO: is it fine to have provider? is it needed at all?
    //  most likely it should be property to create custom sourceSets
    public val platform: Provider<KotlinPlatformType>
    public val languageVersion: Provider<KotlinVersion>
    public val apiVersion: Provider<KotlinVersion>

    // TODO: is those really needed
    //  most likely we need it to create custom sourceSets
    public val classpath: ConfigurableFileCollection
    public val sourceFiles: ConfigurableFileCollection

    // this is really needed
    public val suppressedSourceFiles: ConfigurableFileCollection
    // TODO: suppressGeneratedFiles - not sure that we need it

    public val samples: ConfigurableFileCollection
}
