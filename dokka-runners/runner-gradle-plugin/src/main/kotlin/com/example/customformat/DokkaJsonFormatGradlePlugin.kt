/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package com.example.customformat

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl
import org.jetbrains.dokka.gradle.dsl.formats.DokkaFormatConfiguration
import org.jetbrains.dokka.gradle.dsl.formats.DokkaFormatsContainer

public abstract class DokkaJsonFormatGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        TODO("Not yet implemented")
    }
}

@DokkaGradlePluginDsl
public interface DokkaJsonFormatConfiguration : DokkaFormatConfiguration {
    public val prettyPrint: Property<Boolean>
}

@DokkaGradlePluginDsl
public fun DokkaFormatsContainer.json(configure: DokkaJsonFormatConfiguration.() -> Unit) {

}
