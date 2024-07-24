/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.dependencies

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.model.ObjectFactory
import org.jetbrains.dokka.gradle.DokkaBasePlugin.Companion.DOKKA_CONFIGURATION_NAME
import org.jetbrains.dokka.gradle.DokkaBasePlugin.Companion.DOKKA_GENERATOR_PLUGINS_CONFIGURATION_NAME
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import org.jetbrains.dokka.gradle.internal.declarable


/**
 * Root [Configuration] for fetching all types of Dokkatoo files from other subprojects.
 */
@DokkaInternalApi
class BaseDependencyManager(
    project: Project,
    objects: ObjectFactory,
) {

    internal val baseAttributes: BaseAttributes = BaseAttributes(objects = objects)

    val declaredDependencies: Configuration =
        project.configurations.create(DOKKA_CONFIGURATION_NAME) {
            description = "Fetch all Dokkatoo files from all configurations in other subprojects."
            declarable()
        }

    val dokkaGeneratorPlugins: Configuration =
        project.configurations.create(DOKKA_GENERATOR_PLUGINS_CONFIGURATION_NAME) {
            description = "Dokka Plugins classpath, that will be used by all formats."
            declarable()
        }
}
