package org.jetbrains.dokka.gradle.formats

import org.jetbrains.dokka.gradle.internal.DokkatooInternalApi
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
