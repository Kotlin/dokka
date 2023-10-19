package org.jetbrains.dokka.dokkatoo.adapters

import org.jetbrains.dokka.dokkatoo.DokkatooExtension
import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.*

/**
 * Apply Java specific configuration to the Dokkatoo plugin.
 *
 * **Must be applied *after* [org.jetbrains.dokka.dokkatoo.DokkatooBasePlugin]**
 */
@DokkatooInternalApi
abstract class DokkatooJavaAdapter @Inject constructor() : Plugin<Project> {

  private val logger = Logging.getLogger(this::class.java)

  override fun apply(project: Project) {
    logger.info("applied DokkatooJavaAdapter to ${project.path}")

    // wait for the Java plugin to be applied
    project.plugins.withType<JavaBasePlugin>().configureEach {

      // fetch the toolchain, and use the language version as Dokka's jdkVersion
      val toolchainLanguageVersion = project.extensions.getByType<JavaPluginExtension>()
        .toolchain
        .languageVersion

      val dokka = project.extensions.getByType<DokkatooExtension>()
      dokka.dokkatooSourceSets.configureEach {
        jdkVersion.set(toolchainLanguageVersion.map { it.asInt() }.orElse(8))
      }
    }
  }
}
