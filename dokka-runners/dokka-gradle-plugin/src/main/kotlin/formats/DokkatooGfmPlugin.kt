package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import org.gradle.kotlin.dsl.*

abstract class DokkatooGfmPlugin
@DokkatooInternalApi
constructor() : DokkatooFormatPlugin(formatName = "gfm") {
  override fun DokkatooFormatPluginContext.configure() {
    project.dependencies {
      dokkaPlugin(dokka("gfm-plugin"))
    }
  }
}
