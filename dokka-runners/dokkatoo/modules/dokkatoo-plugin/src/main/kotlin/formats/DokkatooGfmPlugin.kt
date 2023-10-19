package org.jetbrains.dokka.dokkatoo.formats

import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
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
