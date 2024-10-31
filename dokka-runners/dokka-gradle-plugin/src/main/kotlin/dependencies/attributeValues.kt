/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.dependencies

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.named
import org.jetbrains.dokka.gradle.internal.Attribute
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi


/** Common [Attribute] values for Dokka [Configuration]s. */
@InternalDokkaGradlePluginApi
class BaseAttributes(
    objects: ObjectFactory,
) {
    val dokkaUsage: Usage = objects.named("org.jetbrains.dokka")

    val dokkaPlugins: DokkaAttribute.Classpath =
        DokkaAttribute.Classpath("dokka-plugins")

    val dokkaPublicationPlugins: DokkaAttribute.Classpath =
        DokkaAttribute.Classpath("dokka-publication-plugins")

    val dokkaGenerator: DokkaAttribute.Classpath =
        DokkaAttribute.Classpath("dokka-generator")
}


/** [Attribute] values for a specific Dokka format. */
@InternalDokkaGradlePluginApi
class FormatAttributes(
    formatName: String,
) {
    val format: DokkaAttribute.Format =
        DokkaAttribute.Format(formatName)

    val moduleOutputDirectories: DokkaAttribute.ModuleComponent =
        DokkaAttribute.ModuleComponent("ModuleOutputDirectories")
}
