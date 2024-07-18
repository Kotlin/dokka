package org.jetbrains.dokka.gradle.dependencies

import org.jetbrains.dokka.gradle.DokkatooBasePlugin.Companion.DOKKATOO_CONFIGURATION_NAME
import org.jetbrains.dokka.gradle.DokkatooBasePlugin.Companion.DOKKA_GENERATOR_PLUGINS_CONFIGURATION_NAME
import org.jetbrains.dokka.gradle.internal.DokkatooInternalApi
import org.jetbrains.dokka.gradle.internal.declarable
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.model.ObjectFactory


/**
 * Root [Configuration] for fetching all types of Dokkatoo files from other subprojects.
 */
@DokkatooInternalApi
class BaseDependencyManager(
  project: Project,
  objects: ObjectFactory,
) {

  internal val baseAttributes: BaseAttributes = BaseAttributes(objects = objects)

  val declaredDependencies: Configuration =
    project.configurations.create(DOKKATOO_CONFIGURATION_NAME) {
      description = "Fetch all Dokkatoo files from all configurations in other subprojects."
      declarable()
    }

  val dokkaGeneratorPlugins: Configuration =
    project.configurations.create(DOKKA_GENERATOR_PLUGINS_CONFIGURATION_NAME) {
      description = "Dokka Plugins classpath, that will be used by all formats."
      declarable()
    }
}
