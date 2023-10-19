package org.jetbrains.dokka.dokkatoo

import org.jetbrains.dokka.dokkatoo.formats.DokkatooGfmPlugin
import org.jetbrains.dokka.dokkatoo.formats.DokkatooHtmlPlugin
import org.jetbrains.dokka.dokkatoo.formats.DokkatooJavadocPlugin
import org.jetbrains.dokka.dokkatoo.formats.DokkatooJekyllPlugin
import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

/**
 * Dokkatoo Gradle Plugin.
 *
 * Creates all necessary defaults to generate documentation for HTML, Jekyll, Markdown, and Javadoc formats.
 */
abstract class DokkatooPlugin
@DokkatooInternalApi
constructor() : Plugin<Project> {

  override fun apply(target: Project) {
    with(target.pluginManager) {
      apply(type = DokkatooBasePlugin::class)

      // auto-apply the custom format plugins
      apply(type = DokkatooGfmPlugin::class)
      apply(type = DokkatooHtmlPlugin::class)
      apply(type = DokkatooJavadocPlugin::class)
      apply(type = DokkatooJekyllPlugin::class)
    }
  }
}
