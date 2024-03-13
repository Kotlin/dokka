/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("OPT_IN_IS_NOT_ENABLED")

package org.jetbrains.dokka.gradle.internal

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import org.jetbrains.dokka.gradle.dsl.*
import javax.inject.Inject

internal abstract class DefaultDokkaProjectExtension @Inject constructor(
    internal val project: Project,
    objects: ObjectFactory
) : DokkaProjectExtension, DefaultDokkaModuleBasedConfiguration(objects) {
    override val currentProject: DefaultDokkaCurrentProjectConfiguration = objects.newInstance()
    override val aggregation: DefaultDokkaAggregationConfiguration = objects.newInstance()
}

@OptIn(DokkaGradlePluginExperimentalApi::class)
internal abstract class DefaultDokkaModuleBasedConfiguration @Inject constructor(
    objects: ObjectFactory
) : DokkaModuleBasedConfiguration {
    override val html: DokkaHtmlConfiguration = objects.newInstance()
    override val versioning: DokkaVersioningConfiguration = objects.newInstance()
    override val documentationLinks: DefaultDokkaExternalDocumentationLinksConfiguration = objects.newInstance()
}

internal abstract class DefaultDokkaCurrentProjectConfiguration @Inject constructor(
    objects: ObjectFactory
) : DokkaCurrentProjectConfiguration, DefaultDokkaModuleBasedConfiguration(objects)

internal abstract class DefaultDokkaAggregationConfiguration :
    DokkaAggregationConfiguration

internal abstract class DefaultDokkaExternalDocumentationLinksConfiguration :
    DokkaExternalDocumentationLinksConfiguration
