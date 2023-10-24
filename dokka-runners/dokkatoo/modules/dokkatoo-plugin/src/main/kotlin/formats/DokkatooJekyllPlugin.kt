package org.jetbrains.dokka.dokkatoo.formats

import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import org.gradle.kotlin.dsl.*

abstract class DokkatooJekyllPlugin
@DokkatooInternalApi
constructor() : DokkatooFormatPlugin(formatName = "jekyll") {
  override fun DokkatooFormatPluginContext.configure() {
    project.dependencies {
      dokkaPlugin(dokka("jekyll-plugin"))
    }
  }
}
